package org.rdlinux.transactionalmq.rabbitmq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RabbitMQ 统一消费者消息监听器
 */
@Slf4j
class RabbitMqConsumerMessageListener implements ChannelAwareMessageListener {

    /**
     * 消息消费者
     */
    private final TransactionalMessageConsumer<?> consumer;
    /**
     * RabbitMQ 消费调用器
     */
    private final RabbitMqConsumerInvoker rabbitMqConsumerInvoker;
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

    /**
     * 构造不带延迟重试发布服务的监听器
     *
     * @param consumer                  消费者
     * @param rabbitMqConsumerInvoker   RabbitMQ 消费调用器
     * @param messagePayloadSerializer  消息负载序列化器
     * @param consumeIdempotentService  消费幂等服务
     * @param txnMqTransactionalService 事务服务
     */
    RabbitMqConsumerMessageListener(TransactionalMessageConsumer<?> consumer,
                                    RabbitMqConsumerInvoker rabbitMqConsumerInvoker,
                                    MessagePayloadSerializer messagePayloadSerializer,
                                    ConsumeIdempotentService consumeIdempotentService,
                                    TxnMqTransactionalService txnMqTransactionalService) {
        this(consumer, rabbitMqConsumerInvoker, messagePayloadSerializer, consumeIdempotentService,
                txnMqTransactionalService, null);
    }

    /**
     * 构造 RabbitMQ 消费监听器
     *
     * @param consumer                  消费者
     * @param rabbitMqConsumerInvoker   RabbitMQ 消费调用器
     * @param messagePayloadSerializer  消息负载序列化器
     * @param consumeIdempotentService  消费幂等服务
     * @param txnMqTransactionalService 事务服务
     * @param messagePublishService     消息发布服务
     */
    RabbitMqConsumerMessageListener(TransactionalMessageConsumer<?> consumer,
                                    RabbitMqConsumerInvoker rabbitMqConsumerInvoker, MessagePayloadSerializer messagePayloadSerializer,
                                    ConsumeIdempotentService consumeIdempotentService,
                                    TxnMqTransactionalService txnMqTransactionalService,
                                    MessagePublishService messagePublishService) {
        this.consumer = consumer;
        this.rabbitMqConsumerInvoker = rabbitMqConsumerInvoker;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.consumeIdempotentService = consumeIdempotentService;
        this.txnMqTransactionalService = txnMqTransactionalService;
        this.messagePublishService = messagePublishService;
        this.payloadType = this.resolvePayloadType(consumer);
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        //traceId处理
        String traceId = new ObjectId().toHexString();
        MDC.put("X-B3-TraceId", traceId);
        MDC.put("traceId", traceId);
        try {
            ConsumeContext context;
            try {
                context = this.buildContext(message.getMessageProperties());
            } catch (Exception e) {
                log.error("队列{}消息context解析失败", this.consumer.getQueueName(), e);
                this.nAck(message, channel);
                return;
            }
            Object payload;
            try {
                payload = this.deserialize(message);
            } catch (Exception e) {
                log.error("队列{}消息payload解析失败", this.consumer.getQueueName(), e);
                this.nAck(message, channel);
                return;
            }
            AtomicBoolean doAck = new AtomicBoolean(Boolean.FALSE);
            ConsumeHandleContext handleContext = ConsumeHandleContext.DEFAULT();
            AtomicReference<String> failureMessageRef = new AtomicReference<>("consume failed");
            Exception exeException = null;
            try {
                this.invokeBeforeTransaction(context, handleContext, payload);
                this.txnMqTransactionalService.required(() -> {
                    if (!this.consumeIdempotentService.recordIfAbsent(context)) {
                        log.info("队列消息已被处理过, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                                this.consumer.getQueueName(),
                                context.getId(), context.getParentId(), context.getRootId());
                        doAck.set(true);
                        return;
                    }
                    log.info("开始处理队列消息, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                            this.consumer.getQueueName(),
                            context.getId(), context.getParentId(), context.getRootId());
                    this.invokeConsumer(context, handleContext, payload);
                    if (handleContext.isRollBack()) {
                        //回滚前提交ack
                        if (handleContext.isRollBackAck()) {
                            doAck.set(true);
                        }
                        log.info("处理队列事务回滚, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                                this.consumer.getQueueName(),
                                context.getId(), context.getParentId(), context.getRootId());
                        failureMessageRef.set("consumer requested transaction rollback");
                        throw new RuntimeException("处理队列事务回滚");
                    }
                    //事务提交前回调
                    handleContext.executeCommitCall();
                    //不回滚, 提交ack
                    doAck.set(true);
                });
            } catch (UnexpectedRollbackException e) {
                exeException = e;
                log.error("队列消息处理失败, 事务意外回滚, 队列:{}", this.consumer.getQueueName(), e);
                doAck.set(false);
                failureMessageRef.set(this.describeFailure(e));
            } catch (Exception e) {
                exeException = e;
                log.error("队列消息处理失败, 队列:{}", this.consumer.getQueueName(), e);
                failureMessageRef.set(this.describeFailure(e));
            } finally {
                //执行finallyCall
                try {
                    log.info("执行事务提交或者回滚后回调, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                            this.consumer.getQueueName(),
                            context.getId(), context.getParentId(), context.getRootId());
                    handleContext.executeFinallyCall(exeException);
                } catch (Exception ex) {
                    log.error("执行事务提交或者回滚后回调异常, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                            this.consumer.getQueueName(),
                            context.getId(), context.getParentId(), context.getRootId(), ex);
                    doAck.set(false);
                    failureMessageRef.set(this.describeFailure(ex));
                } finally {
                    if (doAck.get()) {
                        log.info("提交ack, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                                this.consumer.getQueueName(),
                                context.getId(), context.getParentId(), context.getRootId());
                        this.ack(message, channel);
                    } else if (this.handleConsumeFailure(message, context, payload, failureMessageRef.get())) {
                        this.ack(message, channel);
                    } else {
                        this.nAck(message, channel);
                    }
                }
            }
        } finally {
            MDC.remove("X-B3-TraceId");
            MDC.remove("traceId");
        }
    }

    /**
     * 确认 RabbitMQ 消息
     *
     * @param message RabbitMQ 消息
     * @param channel RabbitMQ 通道
     */
    private void ack(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("消息ack失败, 队列:{}", message.getMessageProperties().getConsumerQueue(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 否定确认 RabbitMQ 消息并重新入队
     *
     * @param message RabbitMQ 消息
     * @param channel RabbitMQ 通道
     */
    private void nAck(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            channel.basicNack(deliveryTag, false, Boolean.TRUE);
        } catch (IOException e) {
            log.error("消息nack失败, 队列:{}", message.getMessageProperties().getConsumerQueue(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 反序列化 RabbitMQ 消息负载
     *
     * @param message RabbitMQ 消息
     * @return 反序列化后的消息负载
     */
    private Object deserialize(Message message) {
        String payloadText = RabbitMqPayloadCodec.decode(message.getBody(),
                message.getMessageProperties().getContentEncoding());
        return this.messagePayloadSerializer.deserialize(payloadText, this.payloadType);
    }

    /**
     * 调用业务消费者
     *
     * @param context       消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消费负载
     */
    @SuppressWarnings("unchecked")
    private void invokeConsumer(ConsumeContext context, ConsumeHandleContext handleContext, Object payload) {
        this.rabbitMqConsumerInvoker.invoke((TransactionalMessageConsumer<Object>) this.consumer, context,
                handleContext, payload);
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
        this.rabbitMqConsumerInvoker.invokeBeforeTransaction((TransactionalMessageConsumer<Object>) this.consumer,
                context, handleContext, payload);
    }

    /**
     * 从 RabbitMQ 消息属性构建消费上下文
     *
     * @param properties RabbitMQ 消息属性
     * @return 消费上下文
     */
    private ConsumeContext buildContext(MessageProperties properties) {
        String messageId = properties.getMessageId();
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("message id must not be blank");
        }
        Object messageKey = properties.getHeaders().get("messageKey");
        Object parentId = properties.getHeaders().get("parentId");
        Object rootId = properties.getHeaders().get("rootId");
        Object originalMessageId = properties.getHeaders().get("originalMessageId");
        Object retryCount = properties.getHeaders().get("retryCount");
        Map<String, String> headers = this.toHeaders(properties);
        return new ConsumeContext()
                .setId(messageId)
                .setOriginalMessageId(originalMessageId == null ? messageId : String.valueOf(originalMessageId))
                .setRetryCount(this.parseRetryCount(retryCount))
                .setMessageKey(messageKey == null ? null : String.valueOf(messageKey))
                .setParentId(parentId == null ? null : String.valueOf(parentId))
                .setRootId(rootId == null ? messageId : String.valueOf(rootId))
                .setHeaders(headers)
                .setConsumerCode(this.consumer.consumerCode());
    }

    /**
     * 保存下一轮消费重试或停止重试审计记录
     *
     * @param message        RabbitMQ 消息
     * @param context        消费上下文
     * @param payload        消息负载
     * @param failureMessage 失败信息
     * @return 是否可以确认当前 MQ 消息
     */
    private boolean handleConsumeFailure(Message message, ConsumeContext context, Object payload,
                                         String failureMessage) {
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
            TransactionalMessage<Object> retryMessage = this.buildRetryMessage(message, context, payload);
            Optional<Duration> nextDelay = retryPolicy.nextDelay(context.getRetryCount());
            if (nextDelay.isPresent()) {
                this.messagePublishService.scheduleConsumeRetry(MqType.RABBITMQ, retryMessage, context,
                        nextDelay.get(), failureMessage);
            } else {
                this.messagePublishService.recordConsumeRetryStopped(MqType.RABBITMQ, retryMessage, context,
                        failureMessage);
            }
            return true;
        } catch (Exception ex) {
            log.error("队列消息消费重试记录保存失败, 队列:{}, 消息id:{}, 原始消息id:{}, 重试次数:{}",
                    this.consumer.getQueueName(), context.getId(), context.getOriginalMessageId(),
                    context.getRetryCount(), ex);
            return false;
        }
    }

    /**
     * 构建消费重试消息
     *
     * @param sourceMessage RabbitMQ 原消息
     * @param context       消费上下文
     * @param payload       消息负载
     * @return 消费重试消息
     */
    private TransactionalMessage<Object> buildRetryMessage(Message sourceMessage, ConsumeContext context,
                                                           Object payload) {
        MessageProperties properties = sourceMessage.getMessageProperties();
        String destination = this.headerValue(properties, "destination");
        String route = this.headerValue(properties, "route");
        if (destination == null || destination.trim().isEmpty()) {
            String receivedExchange = properties.getReceivedExchange();
            String receivedRoutingKey = properties.getReceivedRoutingKey();
            if (receivedExchange == null || receivedExchange.trim().isEmpty()) {
                destination = receivedRoutingKey == null ? this.consumer.getQueueName() : receivedRoutingKey;
                route = null;
            } else {
                destination = receivedExchange;
                route = receivedRoutingKey;
            }
        }
        return new TransactionalMessage<>()
                .setMessageKey(context.getMessageKey())
                .setProducerCode(this.headerValue(properties, "producerCode"))
                .setDestination(destination)
                .setRoute(route)
                .setShardingKey(this.headerValue(properties, "shardingKey"))
                .setHeaders(context.getHeaders())
                .setBizKey(this.headerValue(properties, "bizKey"))
                .setPayload(payload);
    }

    /**
     * 获取消息头文本
     *
     * @param properties 消息属性
     * @param key        消息头键
     * @return 消息头文本
     */
    private String headerValue(MessageProperties properties, String key) {
        Object value = properties.getHeaders().get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 解析重试次数
     *
     * @param retryCount 重试次数消息头
     * @return 重试次数
     */
    private int parseRetryCount(Object retryCount) {
        if (retryCount == null) {
            return 0;
        }
        int parsedRetryCount = Integer.parseInt(String.valueOf(retryCount));
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
     * 转换 RabbitMQ 消息头
     *
     * @param properties RabbitMQ 消息属性
     * @return 消息头映射
     */
    private Map<String, String> toHeaders(MessageProperties properties) {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.getHeaders().entrySet()) {
            headers.put(entry.getKey(), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        return headers;
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
