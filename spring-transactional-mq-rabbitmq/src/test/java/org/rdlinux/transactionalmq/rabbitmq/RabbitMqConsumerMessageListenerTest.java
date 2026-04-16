package org.rdlinux.transactionalmq.rabbitmq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;

import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;

public class RabbitMqConsumerMessageListenerTest {

    @Test
    public void onMessageShouldRecordDeserializeInvokeAndAck() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        RecordingConsumer consumer = new RecordingConsumer();
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
            consumeIdempotentService);
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-1");
        properties.setDeliveryTag(7L);
        properties.setHeader("messageKey", "key-1");
        properties.setHeader("parentId", "parent-1");
        properties.setHeader("rootId", "root-1");
        properties.setHeader("traceId", "trace-1");
        properties.setContentEncoding("gzip");
        Message message = new Message(RabbitMqPayloadCodec.gzip("\"payload-1\""), properties);

        when(consumeIdempotentService.recordIfAbsent(org.mockito.ArgumentMatchers.any(ConsumeContext.class)))
            .thenReturn(true);
        when(serializer.deserialize("\"payload-1\"", (Type) String.class)).thenReturn("payload-1");

        listener.onMessage(message, channel);

        verify(consumeIdempotentService).recordIfAbsent(org.mockito.ArgumentMatchers.argThat(context ->
                "msg-1".equals(context.getId())
                    && "key-1".equals(context.getMessageKey())
                    && "parent-1".equals(context.getParentId())
                    && "root-1".equals(context.getRootId())
                    && "trace-1".equals(context.getHeaders().get("traceId"))));
        verify(serializer).deserialize("\"payload-1\"", (Type) String.class);
        verify(channel).basicAck(7L, false);
    }

    private static final class RecordingConsumer implements TransactionalMessageConsumer<String> {

        @Override
        public String getQueueName() {
            return "queue.listener";
        }

        @Override
        public String consumerCode() {
            return "consumer-listener";
        }

        @Override
        public void consume(ConsumeContext context, String payload) {
        }
    }
}
