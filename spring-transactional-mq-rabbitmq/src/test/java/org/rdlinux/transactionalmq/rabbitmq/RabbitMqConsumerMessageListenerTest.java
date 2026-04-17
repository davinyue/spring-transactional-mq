package org.rdlinux.transactionalmq.rabbitmq;

import com.rabbitmq.client.Channel;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.QueueMsgHandleRet;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

public class RabbitMqConsumerMessageListenerTest {

    @Test
    public void onMessageShouldRecordDeserializeInvokeAndAck() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        RecordingConsumer consumer = new RecordingConsumer();
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService());
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

    @Test
    public void onMessageShouldNackWithRequeueWhenDeserializeFailed() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        RecordingConsumer consumer = new RecordingConsumer();
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService());
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-2");
        properties.setDeliveryTag(8L);
        Message message = new Message("invalid".getBytes(StandardCharsets.UTF_8), properties);

        when(serializer.deserialize(anyString(), (Type) eq(String.class)))
                .thenThrow(new IllegalArgumentException("bad payload"));

        listener.onMessage(message, channel);

        verify(channel).basicNack(8L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    public void onMessageShouldNackAndKeepInterruptFlagWhenSleepInterrupted() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        InterruptRollbackConsumer consumer = new InterruptRollbackConsumer();
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService());
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-3");
        properties.setDeliveryTag(9L);
        properties.setContentEncoding("gzip");
        Message message = new Message(RabbitMqPayloadCodec.gzip("\"payload-3\""), properties);

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-3\"", (Type) String.class)).thenReturn("payload-3");

        Thread.currentThread().interrupt();
        try {
            listener.onMessage(message, channel);

            verify(channel).basicNack(9L, false, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
            org.junit.Assert.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void onMessageShouldResolveGenericTypeFromAbstractParentConsumer() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        EarlyWarningConsumer consumer = new EarlyWarningConsumer();
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService());
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-4");
        properties.setDeliveryTag(10L);
        properties.setContentEncoding("gzip");
        Message message = new Message(RabbitMqPayloadCodec.gzip("{\"code\":\"ok\"}"), properties);

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("{\"code\":\"ok\"}", (Type) EarlyWarningMessage.class))
                .thenReturn(new EarlyWarningMessage().setCode("ok"));

        listener.onMessage(message, channel);

        verify(serializer).deserialize("{\"code\":\"ok\"}", (Type) EarlyWarningMessage.class);
        verify(channel).basicAck(10L, false);
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
        public QueueMsgHandleRet consume(ConsumeContext context, String payload) {
            return QueueMsgHandleRet.DEFAULT();
        }
    }

    private static final class InterruptRollbackConsumer implements TransactionalMessageConsumer<String> {

        @Override
        public String getQueueName() {
            return "queue.interrupt";
        }

        @Override
        public String consumerCode() {
            return "consumer-interrupt";
        }

        @Override
        public QueueMsgHandleRet consume(ConsumeContext context, String payload) {
            return QueueMsgHandleRet.DEFAULT().setRollBack(true).setRollBackAck(false);
        }
    }

    private abstract static class AbstractParentConsumer<T extends BaseMessage<T>> implements TransactionalMessageConsumer<T> {

        @Override
        public String getQueueName() {
            return "queue.abstract";
        }

        @Override
        public String consumerCode() {
            return "consumer-abstract";
        }
    }

    private static final class EarlyWarningConsumer extends AbstractParentConsumer<EarlyWarningMessage> {

        @Override
        public QueueMsgHandleRet consume(ConsumeContext context, EarlyWarningMessage payload) {
            return QueueMsgHandleRet.DEFAULT();
        }
    }

    private static class BaseMessage<Mt extends BaseMessage<Mt>> {

        private String code;

        public String getCode() {
            return code;
        }

        @SuppressWarnings("unchecked")
        public Mt setCode(String code) {
            this.code = code;
            return (Mt) this;
        }
    }

    private static final class EarlyWarningMessage extends BaseMessage<EarlyWarningMessage> {
    }
}
