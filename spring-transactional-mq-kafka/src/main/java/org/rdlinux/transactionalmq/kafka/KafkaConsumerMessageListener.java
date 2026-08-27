package org.rdlinux.transactionalmq.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.rdlinux.id.objectid.ObjectId;
import org.rdlinux.transactionalmq.api.consumer.ConsumeHandleContext;
import org.rdlinux.transactionalmq.api.consumer.ConsumeRetryPolicy;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.slf4j.MDC;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.lang.NonNull;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Kafka 统一消费者消息监听器。
 */
@Slf4j
class KafkaConsumerMessageListener implements AcknowledgingMessageListener<String, byte[]> {

    /**
     * 消息消费者
     */
    private final TransactionalMessageConsumer<?> consumer;
    /**
     * Kafka 消费调用器
     */
    private final KafkaConsumerInvoker kafkaConsumerInvoker;
    /**
     * 消息负载序列化器
     */
    private final MessagePayloadSerializer messagePayloadSerializer;
    /**
     * 消费幂等服务
     */
    private final ConsumeIdempotentService consumeIdempotentService;
    /**
     * 事务消息事务服务
     */
    private final TxnMqTransactionalService txnMqTransactionalService;
    /**
     * 消息发布服务
     */
    private final MessagePublishService messagePublishService;
    /**
     * 消息负载类型
     */
    private final Type payloadType;

    KafkaConsumerMessageListener(TransactionalMessageConsumer<?> consumer, KafkaConsumerInvoker kafkaConsumerInvoker,
                                 MessagePayloadSerializer messagePayloadSerializer,
                                 ConsumeIdempotentService consumeIdempotentService,
                                 TxnMqTransactionalService txnMqTransactionalService,
                                 MessagePublishService messagePublishService) {
        this.consumer = consumer;
        this.kafkaConsumerInvoker = kafkaConsumerInvoker;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.consumeIdempotentService = consumeIdempotentService;
        this.txnMqTransactionalService = txnMqTransactionalService;
        this.messagePublishService = messagePublishService;
        this.payloadType = this.resolvePayloadType(consumer);
    }

    @Override
    public void onMessage(@NonNull ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) {
        String traceId = new ObjectId().toHexString();
        MDC.put("X-B3-TraceId", traceId);
        MDC.put("traceId", traceId);
        try {
            ConsumeContext context;
            try {
                context = this.buildContext(record);
            } catch (Exception e) {
                log.error("topic {} 消息context解析失败", this.consumer.getQueueName(), e);
                this.nack(acknowledgment);
                return;
            }
            Object payload;
            try {
                payload = this.deserialize(record);
            } catch (Exception e) {
                log.error("topic {} 消息payload解析失败", this.consumer.getQueueName(), e);
                this.nack(acknowledgment);
                return;
            }
            AtomicBoolean doAck = new AtomicBoolean(false);
            AtomicBoolean needRetry = new AtomicBoolean(false);
            ConsumeHandleContext handleContext = ConsumeHandleContext.DEFAULT();
            AtomicReference<String> failureMessageRef = new AtomicReference<>("consume failed");
            Exception exeException = null;
            try {
                this.invokeBeforeTransaction(context, handleContext, payload);
                this.txnMqTransactionalService.required(() -> {
                    if (!this.consumeIdempotentService.recordIfAbsent(context)) {
                        doAck.set(true);
                        return;
                    }
                    this.invokeConsumer(context, handleContext, payload);
                    if (handleContext.isRollBack()) {
                        needRetry.set(true);
                        failureMessageRef.set("consumer requested transaction rollback");
                        throw new RuntimeException("处理队列事务回滚");
                    }
                    handleContext.executeCommitCall();
                    doAck.set(true);
                });
            } catch (UnexpectedRollbackException e) {
                exeException = e;
                log.error("topic 消息处理失败, 事务意外回滚, topic:{}", this.consumer.getQueueName(), e);
                doAck.set(false);
                needRetry.set(true);
                failureMessageRef.set(this.describeFailure(e));
            } catch (Exception e) {
                exeException = e;
                log.error("topic 消息处理失败, topic:{}", this.consumer.getQueueName(), e);
                needRetry.set(true);
                failureMessageRef.set(this.describeFailure(e));
            } finally {
                try {
                    handleContext.executeFinallyCall(exeException);
                } catch (Exception ex) {
                    log.error("执行事务提交或者回滚后回调异常, topic:{}", this.consumer.getQueueName(), ex);
                    doAck.set(false);
                    needRetry.set(true);
                    failureMessageRef.set(this.describeFailure(ex));
                } finally {
                    if (doAck.get()) {
                        acknowledgment.acknowledge();
                    } else if (needRetry.get()
                            && this.handleConsumeFailure(record, context, payload, failureMessageRef.get())) {
                        acknowledgment.acknowledge();
                    } else {
                        this.nack(acknowledgment);
                    }
                }
            }
        } finally {
            MDC.remove("X-B3-TraceId");
            MDC.remove("traceId");
        }
    }

    /**
     * 否定确认 Kafka 消息
     *
     * @param acknowledgment Kafka 消息确认器
     */
    private void nack(Acknowledgment acknowledgment) {
        if (acknowledgment != null) {
            acknowledgment.nack(Duration.ofMillis(10000L));
        }
    }

    /**
     * 保存下一轮消费重试或停止重试审计记录
     *
     * @param record         Kafka 原消息
     * @param context        消费上下文
     * @param payload        消息负载
     * @param failureMessage 失败信息
     * @return 是否可以提交当前消息 offset
     */
    private boolean handleConsumeFailure(ConsumerRecord<String, byte[]> record, ConsumeContext context,
                                         Object payload, String failureMessage) {
        if (this.messagePublishService == null) {
            return false;
        }
        try {
            ConsumeRetryPolicy retryPolicy = this.consumer.getConsumeRetryPolicy();
            if (retryPolicy == null) {
                throw new IllegalStateException("consume retry policy must not be null");
            }
            if (retryPolicy.isNativeNack()) {
                return false;
            }
            TransactionalMessage<Object> retryMessage = new TransactionalMessage<>()
                    .setMessageKey(context.getMessageKey())
                    .setProducerCode(this.findHeader(record, "producerCode"))
                    .setDestination(record.topic())
                    .setRoute(record.key())
                    .setShardingKey(this.findHeader(record, "shardingKey"))
                    .setHeaders(this.toHeaders(record))
                    .setBizKey(this.findHeader(record, "bizKey"))
                    .setPayload(payload);
            Optional<Duration> nextDelay = retryPolicy.nextDelay(context.getRetryCount());
            if (nextDelay.isPresent()) {
                this.messagePublishService.scheduleConsumeRetry(MqType.KAFKA, retryMessage, context,
                        nextDelay.get(), failureMessage);
            } else {
                this.messagePublishService.recordConsumeRetryStopped(MqType.KAFKA, retryMessage, context,
                        failureMessage);
            }
            return true;
        } catch (Exception ex) {
            log.error("topic 消息消费重试记录保存失败, topic:{}, 消息id:{}, 原始消息id:{}, 重试次数:{}",
                    this.consumer.getQueueName(), context.getId(), context.getOriginalMessageId(),
                    context.getRetryCount(), ex);
            return false;
        }
    }

    /**
     * 反序列化 Kafka 消息负载
     *
     * @param record Kafka 消息记录
     * @return 反序列化后的消息负载
     */
    private Object deserialize(ConsumerRecord<String, byte[]> record) {
        String payloadText = KafkaPayloadCodec.decode(record.value(), this.findHeader(record, "contentEncoding"));
        return this.messagePayloadSerializer.deserialize(payloadText, this.payloadType);
    }

    /**
     * 调用业务消费者
     *
     * @param context       消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消息负载
     */
    @SuppressWarnings("unchecked")
    private void invokeConsumer(ConsumeContext context, ConsumeHandleContext handleContext, Object payload) {
        this.kafkaConsumerInvoker.invoke((TransactionalMessageConsumer<Object>) this.consumer, context, handleContext,
                payload);
    }

    /**
     * 调用事务开启前的消费者处理逻辑
     *
     * @param context       消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消息负载
     */
    @SuppressWarnings("unchecked")
    private void invokeBeforeTransaction(ConsumeContext context, ConsumeHandleContext handleContext, Object payload) {
        this.kafkaConsumerInvoker.invokeBeforeTransaction((TransactionalMessageConsumer<Object>) this.consumer,
                context, handleContext, payload);
    }

    /**
     * 从 Kafka 消息构建消费上下文
     *
     * @param record Kafka 消息记录
     * @return 消费上下文
     */
    private ConsumeContext buildContext(ConsumerRecord<String, byte[]> record) {
        String messageId = this.findHeader(record, "messageId");
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("message id must not be blank");
        }
        String rootId = this.findHeader(record, "rootId");
        String originalMessageId = this.findHeader(record, "originalMessageId");
        return new ConsumeContext()
                .setId(messageId)
                .setOriginalMessageId(originalMessageId == null || originalMessageId.trim().isEmpty()
                        ? messageId : originalMessageId)
                .setRetryCount(this.parseRetryCount(this.findHeader(record, "retryCount")))
                .setMessageKey(this.findHeader(record, "messageKey"))
                .setParentId(this.findHeader(record, "parentId"))
                .setRootId(rootId == null || rootId.trim().isEmpty() ? messageId : rootId)
                .setHeaders(this.toHeaders(record))
                .setConsumerCode(this.consumer.consumerCode());
    }

    /**
     * 解析重试次数
     *
     * @param retryCount 重试次数消息头
     * @return 重试次数
     */
    private int parseRetryCount(String retryCount) {
        if (retryCount == null || retryCount.trim().isEmpty()) {
            return 0;
        }
        int parsedRetryCount = Integer.parseInt(retryCount);
        if (parsedRetryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        return parsedRetryCount;
    }

    /**
     * 构建失败描述
     *
     * @param throwable 失败异常
     * @return 失败描述
     */
    private String describeFailure(Throwable throwable) {
        if (throwable == null) {
            return "consume failed";
        }
        String message = throwable.getMessage();
        return throwable.getClass().getName() + (message == null ? "" : ": " + message);
    }

    /**
     * 转换 Kafka 消息头
     *
     * @param record Kafka 消息记录
     * @return 消息头映射
     */
    private Map<String, String> toHeaders(ConsumerRecord<String, byte[]> record) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8));
        }
        return headers;
    }

    /**
     * 读取 Kafka 消息头
     *
     * @param record Kafka 消息记录
     * @param key 消息头名称
     * @return 消息头值，不存在时返回 null
     */
    private String findHeader(ConsumerRecord<String, byte[]> record, String key) {
        Header header = record.headers().lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    /**
     * 解析消费者声明的负载类型
     *
     * @param consumer 消费者
     * @return 负载类型
     */
    private Type resolvePayloadType(TransactionalMessageConsumer<?> consumer) {
        Class<?> userClass = ClassUtils.getUserClass(consumer);
        Class<?> resolvedClass = GenericTypeResolver.resolveTypeArgument(userClass, TransactionalMessageConsumer.class);
        if (resolvedClass != null) {
            return resolvedClass;
        }
        Class<?> current = userClass;
        while (current != null && current != Object.class) {
            ResolvableType resolvableType = ResolvableType.forClass(current)
                    .as(TransactionalMessageConsumer.class);
            ResolvableType payloadResolvableType = resolvableType.getGeneric(0);
            if (payloadResolvableType != ResolvableType.NONE && payloadResolvableType.resolve() != null) {
                return payloadResolvableType.getType();
            }
            current = current.getSuperclass();
        }
        throw new IllegalArgumentException("unable to resolve consumer payload type: " + consumer.getClass().getName());
    }
}
