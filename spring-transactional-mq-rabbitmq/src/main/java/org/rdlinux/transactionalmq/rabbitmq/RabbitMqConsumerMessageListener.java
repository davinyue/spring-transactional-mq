package org.rdlinux.transactionalmq.rabbitmq;

import java.lang.reflect.Type;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.core.ResolvableType;

import com.rabbitmq.client.Channel;

/**
 * RabbitMQ 统一消费者消息监听器。
 */
class RabbitMqConsumerMessageListener implements ChannelAwareMessageListener {

    private final TransactionalMessageConsumer<?> consumer;
    private final RabbitMqConsumerInvoker rabbitMqConsumerInvoker;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final ConsumeIdempotentService consumeIdempotentService;
    private final Type payloadType;

    RabbitMqConsumerMessageListener(TransactionalMessageConsumer<?> consumer,
            RabbitMqConsumerInvoker rabbitMqConsumerInvoker, MessagePayloadSerializer messagePayloadSerializer,
            ConsumeIdempotentService consumeIdempotentService) {
        this.consumer = consumer;
        this.rabbitMqConsumerInvoker = rabbitMqConsumerInvoker;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.consumeIdempotentService = consumeIdempotentService;
        this.payloadType = ResolvableType.forClass(consumer.getClass())
            .as(TransactionalMessageConsumer.class)
            .getGeneric(0)
            .getType();
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        ConsumeContext context = buildContext(message.getMessageProperties());
        if (!this.consumeIdempotentService.recordIfAbsent(context)) {
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            Object payload = deserialize(message);
            invokeConsumer(context, payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            channel.basicNack(deliveryTag, false, false);
            throw ex;
        }
    }

    private Object deserialize(Message message) {
        String payloadText = RabbitMqPayloadCodec.decode(message.getBody(),
            message.getMessageProperties().getContentEncoding());
        return this.messagePayloadSerializer.deserialize(payloadText, this.payloadType);
    }

    @SuppressWarnings("unchecked")
    private void invokeConsumer(ConsumeContext context, Object payload) {
        this.rabbitMqConsumerInvoker.invoke((TransactionalMessageConsumer<Object>) this.consumer, context, payload);
    }

    private ConsumeContext buildContext(MessageProperties properties) {
        String messageId = properties.getMessageId();
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("message id must not be blank");
        }
        Object messageKey = properties.getHeaders().get("messageKey");
        return new ConsumeContext()
            .setId(messageId)
            .setMessageKey(messageKey == null ? null : String.valueOf(messageKey))
            .setConsumerCode(this.consumer.consumerCode());
    }
}
