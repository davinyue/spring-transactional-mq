package org.rdlinux.transactionalmq.rabbitmq;

import com.rabbitmq.client.Channel;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.ConsumeRetryPolicy;
import org.rdlinux.transactionalmq.api.consumer.ConsumeHandleContext;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        assertNull(consumer.finallyException);
    }

    @Test
    public void onMessageShouldInvokeBeforeTransactionBeforeOpeningTransaction() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        List<String> events = new ArrayList<String>();
        BeforeTransactionConsumer consumer = new BeforeTransactionConsumer(events, false);
        TxnMqTransactionalService txnService = mock(TxnMqTransactionalService.class);
        doAnswer(invocation -> {
            events.add("transaction");
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(txnService).required(any(Runnable.class));
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, txnService);
        Channel channel = mock(Channel.class);
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-before");
        properties.setDeliveryTag(13L);
        properties.setContentEncoding("gzip");
        Message message = new Message(RabbitMqPayloadCodec.gzip("\"payload-before\""), properties);

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-before\"", (Type) String.class)).thenReturn("payload-before");

        listener.onMessage(message, channel);

        assertEquals(3, events.size());
        assertEquals("before", events.get(0));
        assertEquals("transaction", events.get(1));
        assertEquals("consume", events.get(2));
        verify(channel).basicAck(13L, false);
    }

    @Test
    public void onMessageShouldNackWhenBeforeTransactionThrows() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        BeforeTransactionConsumer consumer = new BeforeTransactionConsumer(new ArrayList<String>(), true);
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService());
        Channel channel = mock(Channel.class);
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-before-failed");
        properties.setDeliveryTag(14L);
        properties.setContentEncoding("gzip");
        Message message = new Message(RabbitMqPayloadCodec.gzip("\"payload-before-failed\""), properties);

        when(serializer.deserialize("\"payload-before-failed\"", (Type) String.class))
                .thenReturn("payload-before-failed");

        listener.onMessage(message, channel);

        verify(consumeIdempotentService, never()).recordIfAbsent(any(ConsumeContext.class));
        verify(channel).basicNack(14L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        assertFalse(consumer.consumeCalled);
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
    public void onMessageShouldNackWhenConsumerThrowsWithDefaultPolicy() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        ExceptionConsumer consumer = new ExceptionConsumer();
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-exception");
        properties.setDeliveryTag(12L);
        properties.setContentEncoding("gzip");
        Message message = new Message(RabbitMqPayloadCodec.gzip("\"payload-exception\""), properties);

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-exception\"", (Type) String.class))
                .thenReturn("payload-exception");

        listener.onMessage(message, channel);

        verify(channel).basicNack(12L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verifyNoInteractions(messagePublishService);
        assertNotNull(consumer.handleContext);
        assertNotNull(consumer.finallyException);
    }

    @Test
    public void onMessageShouldNackWithoutBlockingWhenRetryPersistenceIsUnavailable() throws Exception {
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

        listener.onMessage(message, channel);

        verify(channel).basicNack(9L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        assertNotNull(consumer.finallyException);
    }

    @Test
    public void onMessageShouldScheduleRetryAndAckWhenBusinessRollback() throws Exception {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        RetryRollbackConsumer consumer = new RetryRollbackConsumer();
        RabbitMqConsumerMessageListener listener = new RabbitMqConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-5");
        properties.setDeliveryTag(11L);
        properties.setHeader("messageKey", "key-5");
        properties.setHeader("originalMessageId", "original-5");
        properties.setHeader("retryCount", 1);
        properties.setHeader("destination", "exchange.demo");
        properties.setHeader("route", "route.demo");
        properties.setHeader("parentId", "parent-5");
        properties.setHeader("rootId", "root-5");
        properties.setContentEncoding("gzip");
        Message message = new Message(RabbitMqPayloadCodec.gzip("\"payload-5\""), properties);

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-5\"", (Type) String.class)).thenReturn("payload-5");
        when(messagePublishService.scheduleConsumeRetry(eq(MqType.RABBITMQ), any(TransactionalMessage.class),
                any(ConsumeContext.class), eq(Duration.ofMinutes(4L)), anyString())).thenReturn(true);

        listener.onMessage(message, channel);

        verify(messagePublishService).scheduleConsumeRetry(eq(MqType.RABBITMQ),
                org.mockito.ArgumentMatchers.argThat((TransactionalMessage<String> retryMessage) ->
                        "exchange.demo".equals(retryMessage.getDestination())
                                && "route.demo".equals(retryMessage.getRoute())
                                && "payload-5".equals(retryMessage.getPayload())),
                org.mockito.ArgumentMatchers.argThat(context ->
                        "msg-5".equals(context.getId())
                                && "original-5".equals(context.getOriginalMessageId())
                                && context.getRetryCount() == 1
                                && "parent-5".equals(context.getParentId())
                                && "root-5".equals(context.getRootId())),
                eq(Duration.ofMinutes(4L)), contains("RuntimeException"));
        verify(channel).basicAck(11L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
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

        private Exception finallyException;

        @Override
        public String getQueueName() {
            return "queue.listener";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public String consumerCode() {
            return "consumer-listener";
        }

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            handleContext.addFinallyCall(exception -> this.finallyException = exception);
        }
    }

    private static final class BeforeTransactionConsumer implements TransactionalMessageConsumer<String> {

        private final List<String> events;
        private final boolean failBeforeTransaction;
        private boolean consumeCalled;

        private BeforeTransactionConsumer(List<String> events, boolean failBeforeTransaction) {
            this.events = events;
            this.failBeforeTransaction = failBeforeTransaction;
        }

        @Override
        public String getQueueName() {
            return "queue.before-transaction";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public String consumerCode() {
            return "consumer-before-transaction";
        }

        @Override
        public void beforeTransaction(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            this.events.add("before");
            if (this.failBeforeTransaction) {
                throw new IllegalStateException("before transaction failed");
            }
        }

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            this.events.add("consume");
            this.consumeCalled = true;
        }
    }

    private static final class ExceptionConsumer implements TransactionalMessageConsumer<String> {

        private ConsumeHandleContext handleContext;
        private Exception finallyException;

        @Override
        public String getQueueName() {
            return "queue.exception";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public String consumerCode() {
            return "consumer-exception";
        }

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            this.handleContext = handleContext;
            handleContext.addFinallyCall(exception -> this.finallyException = exception);
            throw new IllegalStateException("consumer exception");
        }
    }

    private static final class InterruptRollbackConsumer implements TransactionalMessageConsumer<String> {

        private Exception finallyException;

        @Override
        public String getQueueName() {
            return "queue.interrupt";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public String consumerCode() {
            return "consumer-interrupt";
        }

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            handleContext.setRollBack(true)
                    .setRollBackAck(false)
                    .addFinallyCall(exception -> this.finallyException = exception);
        }
    }

    private static final class RetryRollbackConsumer implements TransactionalMessageConsumer<String> {

        @Override
        public String getQueueName() {
            return "queue.retry";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public String consumerCode() {
            return "consumer-retry";
        }

        @Override
        public ConsumeRetryPolicy getConsumeRetryPolicy() {
            return ConsumeRetryPolicy.customDelays(Duration.ofMinutes(2L), Duration.ofMinutes(4L));
        }

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            handleContext.setRollBack(true);
        }
    }

    private abstract static class AbstractParentConsumer<T extends BaseMessage<T>> implements TransactionalMessageConsumer<T> {

        @Override
        public String getQueueName() {
            return "queue.abstract";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public String consumerCode() {
            return "consumer-abstract";
        }
    }

    private static final class EarlyWarningConsumer extends AbstractParentConsumer<EarlyWarningMessage> {

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, EarlyWarningMessage payload) {
        }
    }

    private static class BaseMessage<Mt extends BaseMessage<Mt>> {

        private String code;

        public String getCode() {
            return this.code;
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
