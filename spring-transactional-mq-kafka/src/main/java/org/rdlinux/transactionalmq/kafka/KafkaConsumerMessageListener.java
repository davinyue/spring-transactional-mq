package org.rdlinux.transactionalmq.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.rdlinux.id.objectid.ObjectId;
import org.rdlinux.transactionalmq.api.consumer.QueueMsgHandleRet;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Kafka 统一消费者消息监听器。
 */
@Slf4j
class KafkaConsumerMessageListener implements AcknowledgingMessageListener<String, byte[]> {

    private final TransactionalMessageConsumer<?> consumer;
    private final KafkaConsumerInvoker kafkaConsumerInvoker;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final ConsumeIdempotentService consumeIdempotentService;
    private final TxnMqTransactionalService txnMqTransactionalService;
    private final MessagePublishService messagePublishService;
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
            AtomicBoolean needRepublish = new AtomicBoolean(false);
            AtomicReference<QueueMsgHandleRet> retRef = new AtomicReference<>();
            try {
                this.txnMqTransactionalService.required(() -> {
                    if (!this.consumeIdempotentService.recordIfAbsent(context)) {
                        doAck.set(true);
                        return;
                    }
                    QueueMsgHandleRet handleRet = this.invokeConsumer(context, payload);
                    if (handleRet == null) {
                        handleRet = QueueMsgHandleRet.DEFAULT();
                    }
                    retRef.set(handleRet);
                    if (handleRet.isRollBack()) {
                        needRepublish.set(true);
                        throw new RuntimeException("处理队列事务回滚");
                    }
                    handleRet.executeCommitCall();
                    doAck.set(true);
                });
            } catch (UnexpectedRollbackException e) {
                log.error("topic 消息处理失败, 事务意外回滚, topic:{}", this.consumer.getQueueName(), e);
                doAck.set(false);
            } catch (Exception e) {
                log.error("topic 消息处理失败, topic:{}", this.consumer.getQueueName(), e);
                needRepublish.set(true);
            } finally {
                try {
                    QueueMsgHandleRet handleRet = retRef.get();
                    if (handleRet != null) {
                        handleRet.executeFinallyCall();
                    }
                } catch (Exception ex) {
                    log.error("执行事务提交或者回滚后回调异常, topic:{}", this.consumer.getQueueName(), ex);
                    doAck.set(false);
                } finally {
                    if (doAck.get()) {
                        acknowledgment.acknowledge();
                    } else if (needRepublish.get() && this.republishToTail(record, context, payload)) {
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

    private void nack(Acknowledgment acknowledgment) {
        if (acknowledgment != null) {
            acknowledgment.nack(Duration.ofMillis(10000L));
        }
    }

    private boolean republishToTail(ConsumerRecord<String, byte[]> record, ConsumeContext context, Object payload) {
        if (this.messagePublishService == null) {
            return false;
        }
        try {
            TransactionalMessage<Object> retryMessage = new TransactionalMessage<>()
                    .setMessageKey(context.getMessageKey())
                    .setProducerCode(this.findHeader(record, "producerCode"))
                    .setDestination(record.topic())
                    .setRoute(record.key())
                    .setShardingKey(this.findHeader(record, "shardingKey"))
                    .setHeaders(this.toHeaders(record))
                    .setBizKey(this.findHeader(record, "bizKey"))
                    .setPayload(payload);
            this.messagePublishService.sendWithParent(MqType.KAFKA, retryMessage, context);
            return true;
        } catch (Exception ex) {
            log.error("topic 消息重投到末尾失败, topic:{}", this.consumer.getQueueName(), ex);
            return false;
        }
    }

    private Object deserialize(ConsumerRecord<String, byte[]> record) {
        String payloadText = KafkaPayloadCodec.decode(record.value(), this.findHeader(record, "contentEncoding"));
        return this.messagePayloadSerializer.deserialize(payloadText, this.payloadType);
    }

    @SuppressWarnings("unchecked")
    private QueueMsgHandleRet invokeConsumer(ConsumeContext context, Object payload) {
        return this.kafkaConsumerInvoker.invoke((TransactionalMessageConsumer<Object>) this.consumer, context, payload);
    }

    private ConsumeContext buildContext(ConsumerRecord<String, byte[]> record) {
        String messageId = this.findHeader(record, "messageId");
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("message id must not be blank");
        }
        String rootId = this.findHeader(record, "rootId");
        return new ConsumeContext()
                .setId(messageId)
                .setMessageKey(this.findHeader(record, "messageKey"))
                .setParentId(this.findHeader(record, "parentId"))
                .setRootId(rootId == null || rootId.trim().isEmpty() ? messageId : rootId)
                .setHeaders(this.toHeaders(record))
                .setConsumerCode(this.consumer.consumerCode());
    }

    private Map<String, String> toHeaders(ConsumerRecord<String, byte[]> record) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8));
        }
        return headers;
    }

    private String findHeader(ConsumerRecord<String, byte[]> record, String key) {
        Header header = record.headers().lastHeader(key);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

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
