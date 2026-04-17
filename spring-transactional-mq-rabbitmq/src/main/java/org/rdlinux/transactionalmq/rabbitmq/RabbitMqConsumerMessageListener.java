package org.rdlinux.transactionalmq.rabbitmq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.rdlinux.id.objectid.ObjectId;
import org.rdlinux.transactionalmq.api.consumer.QueueMsgHandleRet;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RabbitMQ 统一消费者消息监听器。
 */
@Slf4j
class RabbitMqConsumerMessageListener implements ChannelAwareMessageListener {

    private final TransactionalMessageConsumer<?> consumer;
    private final RabbitMqConsumerInvoker rabbitMqConsumerInvoker;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final ConsumeIdempotentService consumeIdempotentService;
    private final TxnMqTransactionalService txnMqTransactionalService;
    private final Type payloadType;

    RabbitMqConsumerMessageListener(TransactionalMessageConsumer<?> consumer,
                                    RabbitMqConsumerInvoker rabbitMqConsumerInvoker, MessagePayloadSerializer messagePayloadSerializer,
                                    ConsumeIdempotentService consumeIdempotentService, TxnMqTransactionalService txnMqTransactionalService) {
        this.consumer = consumer;
        this.rabbitMqConsumerInvoker = rabbitMqConsumerInvoker;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.consumeIdempotentService = consumeIdempotentService;
        this.txnMqTransactionalService = txnMqTransactionalService;
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
                this.requeueLater(message, channel, new ConsumeContext());
                return;
            }
            Object payload;
            try {
                payload = this.deserialize(message);
            } catch (Exception e) {
                log.error("队列{}消息payload解析失败", this.consumer.getQueueName(), e);
                this.requeueLater(message, channel, context);
                return;
            }
            AtomicBoolean doAck = new AtomicBoolean(Boolean.FALSE);
            AtomicReference<QueueMsgHandleRet> retRef = new AtomicReference<>();
            try {
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
                    QueueMsgHandleRet handleRet = this.invokeConsumer(context, payload);
                    if (handleRet == null) {
                        handleRet = QueueMsgHandleRet.DEFAULT();
                    }
                    retRef.set(handleRet);
                    if (handleRet.isRollBack()) {
                        //回滚前提交ack
                        if (handleRet.isRollBackAck()) {
                            doAck.set(true);
                        }
                        log.info("处理队列事务回滚, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                                this.consumer.getQueueName(),
                                context.getId(), context.getParentId(), context.getRootId());
                        throw new RuntimeException("处理队列事务回滚");
                    }
                    //事务提交前回调
                    handleRet.executeCommitCall();
                    //不回滚, 提交ack
                    doAck.set(true);
                });
            } catch (UnexpectedRollbackException e) {
                log.error("队列消息处理失败, 事务意外回滚, 队列:{}", this.consumer.getQueueName(), e);
                doAck.set(false);
            } catch (Exception e) {
                log.error("队列消息处理失败, 队列:{}", this.consumer.getQueueName(), e);
            } finally {
                //执行finallyCall
                try {
                    QueueMsgHandleRet handleRet = retRef.get();
                    if (handleRet != null) {
                        log.info("执行事务提交或者回滚后回调, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                                this.consumer.getQueueName(),
                                context.getId(), context.getParentId(), context.getRootId());
                        handleRet.executeFinallyCall();
                    }
                } catch (Exception ex) {
                    log.error("执行事务提交或者回滚后回调异常, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                            this.consumer.getQueueName(),
                            context.getId(), context.getParentId(), context.getRootId(), ex);
                    doAck.set(false);
                } finally {
                    if (doAck.get()) {
                        log.info("提交ack, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                                this.consumer.getQueueName(),
                                context.getId(), context.getParentId(), context.getRootId());
                        this.ack(message, channel);
                    } else {
                        this.requeueLater(message, channel, context);
                    }
                }
            }
        } finally {
            MDC.remove("X-B3-TraceId");
            MDC.remove("traceId");
        }
    }

    private void ack(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("消息ack失败, 队列:{}", message.getMessageProperties().getConsumerQueue(), e);
            throw new RuntimeException(e);
        }
    }

    private void nAck(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            channel.basicNack(deliveryTag, false, Boolean.TRUE);
        } catch (IOException e) {
            log.error("消息nack失败, 队列:{}", message.getMessageProperties().getConsumerQueue(), e);
            throw new RuntimeException(e);
        }
    }

    private void requeueLater(Message message, Channel channel, ConsumeContext context) {
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待重入队列期间线程被中断, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                    this.consumer.getQueueName(),
                    context.getId(), context.getParentId(), context.getRootId(), e);
        }
        log.info("拒绝ack重入队列, 队列:{}, 消息id:{}, 上级消息id:{}, 根消息id:{}",
                this.consumer.getQueueName(),
                context.getId(), context.getParentId(), context.getRootId());
        this.nAck(message, channel);
    }

    private Object deserialize(Message message) {
        String payloadText = RabbitMqPayloadCodec.decode(message.getBody(),
                message.getMessageProperties().getContentEncoding());
        return this.messagePayloadSerializer.deserialize(payloadText, this.payloadType);
    }

    @SuppressWarnings("unchecked")
    private QueueMsgHandleRet invokeConsumer(ConsumeContext context, Object payload) {
        return this.rabbitMqConsumerInvoker.invoke((TransactionalMessageConsumer<Object>) this.consumer, context,
                payload);
    }

    private ConsumeContext buildContext(MessageProperties properties) {
        String messageId = properties.getMessageId();
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("message id must not be blank");
        }
        Object messageKey = properties.getHeaders().get("messageKey");
        Object parentId = properties.getHeaders().get("parentId");
        Object rootId = properties.getHeaders().get("rootId");
        Map<String, String> headers = this.toHeaders(properties);
        return new ConsumeContext()
                .setId(messageId)
                .setMessageKey(messageKey == null ? null : String.valueOf(messageKey))
                .setParentId(parentId == null ? null : String.valueOf(parentId))
                .setRootId(rootId == null ? messageId : String.valueOf(rootId))
                .setHeaders(headers)
                .setConsumerCode(this.consumer.consumerCode());
    }

    private Map<String, String> toHeaders(MessageProperties properties) {
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : properties.getHeaders().entrySet()) {
            headers.put(entry.getKey(), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        return headers;
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
