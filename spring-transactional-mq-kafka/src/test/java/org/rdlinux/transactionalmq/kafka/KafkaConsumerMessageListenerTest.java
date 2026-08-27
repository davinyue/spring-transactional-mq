package org.rdlinux.transactionalmq.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
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
import org.springframework.kafka.support.Acknowledgment;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyString;

public class KafkaConsumerMessageListenerTest {

    @Test
    public void onMessageShouldRecordDeserializeInvokeAndAck() {
        KafkaConsumerInvoker invoker = new KafkaConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        RecordingConsumer consumer = new RecordingConsumer();
        KafkaConsumerMessageListener listener = new KafkaConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<String, byte[]> record = this.buildRecord("msg-1", "key-1", "parent-1", "root-1",
                KafkaPayloadCodec.gzip("\"payload-1\""));

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-1\"", (Type) String.class)).thenReturn("payload-1");

        listener.onMessage(record, acknowledgment);

        verify(consumeIdempotentService).recordIfAbsent(org.mockito.ArgumentMatchers.argThat(context ->
                "msg-1".equals(context.getId())
                        && "key-1".equals(context.getMessageKey())
                        && "parent-1".equals(context.getParentId())
                        && "root-1".equals(context.getRootId())
                        && "trace-1".equals(context.getHeaders().get("traceId"))));
        verify(serializer).deserialize("\"payload-1\"", (Type) String.class);
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(messagePublishService);
        assertNull(consumer.finallyException);
    }

    @Test
    public void onMessageShouldNackWhenDeserializeFailed() {
        KafkaConsumerInvoker invoker = new KafkaConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        RecordingConsumer consumer = new RecordingConsumer();
        KafkaConsumerMessageListener listener = new KafkaConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<String, byte[]> record = this.buildRecord("msg-2", "key-2", null, null,
                "invalid".getBytes(StandardCharsets.UTF_8));

        when(serializer.deserialize(anyString(), (Type) eq(String.class)))
                .thenThrow(new IllegalArgumentException("bad payload"));

        listener.onMessage(record, acknowledgment);

        verify(acknowledgment).nack(Duration.ofMillis(10000L));
        verify(acknowledgment, never()).acknowledge();
        verifyNoInteractions(messagePublishService);
    }

    @Test
    public void onMessageShouldNackWhenConsumerThrowsWithDefaultPolicy() {
        KafkaConsumerInvoker invoker = new KafkaConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        ExceptionConsumer consumer = new ExceptionConsumer();
        KafkaConsumerMessageListener listener = new KafkaConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<String, byte[]> record = this.buildRecord("msg-exception", "key-exception", null, null,
                KafkaPayloadCodec.gzip("\"payload-exception\""));

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-exception\"", (Type) String.class))
                .thenReturn("payload-exception");

        listener.onMessage(record, acknowledgment);

        verify(acknowledgment).nack(Duration.ofMillis(10000L));
        verify(acknowledgment, never()).acknowledge();
        verifyNoInteractions(messagePublishService);
        assertNotNull(consumer.handleContext);
        assertNotNull(consumer.finallyException);
    }

    @Test
    public void onMessageShouldResolveGenericTypeFromAbstractParentConsumer() {
        KafkaConsumerInvoker invoker = new KafkaConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        EarlyWarningConsumer consumer = new EarlyWarningConsumer();
        KafkaConsumerMessageListener listener = new KafkaConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<String, byte[]> record = this.buildRecord("msg-4", "key-4", null, null,
                KafkaPayloadCodec.gzip("{\"code\":\"ok\"}"));

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("{\"code\":\"ok\"}", (Type) EarlyWarningMessage.class))
                .thenReturn(new EarlyWarningMessage().setCode("ok"));

        listener.onMessage(record, acknowledgment);

        verify(serializer).deserialize("{\"code\":\"ok\"}", (Type) EarlyWarningMessage.class);
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(messagePublishService);
    }

    @Test
    public void onMessageShouldScheduleRetryAndAckWhenBusinessRollback() {
        KafkaConsumerInvoker invoker = new KafkaConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        RollbackConsumer consumer = new RollbackConsumer();
        KafkaConsumerMessageListener listener = new KafkaConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<String, byte[]> record = this.buildRecord("msg-5", "key-5", "parent-5", "root-5",
                KafkaPayloadCodec.gzip("\"payload-5\""));

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-5\"", (Type) String.class)).thenReturn("payload-5");
        when(messagePublishService.scheduleConsumeRetry(eq(MqType.KAFKA), any(TransactionalMessage.class),
                any(ConsumeContext.class), eq(Duration.ofMinutes(2L)), anyString())).thenReturn(true);

        listener.onMessage(record, acknowledgment);

        verify(messagePublishService).scheduleConsumeRetry(eq(MqType.KAFKA),
                org.mockito.ArgumentMatchers.argThat((TransactionalMessage<String> message) ->
                        "topic.demo".equals(message.getDestination())
                                && "key-5".equals(message.getRoute())
                                && "payload-5".equals(message.getPayload())
                                && "key-5".equals(message.getHeaders().get("messageKey"))
                                && "trace-1".equals(message.getHeaders().get("traceId"))),
                org.mockito.ArgumentMatchers.argThat(context ->
                        "msg-5".equals(context.getId())
                                && "msg-5".equals(context.getOriginalMessageId())
                                && context.getRetryCount() == 0
                                && "key-5".equals(context.getMessageKey())
                                && "parent-5".equals(context.getParentId())
                                && "root-5".equals(context.getRootId())),
                eq(Duration.ofMinutes(2L)), contains("RuntimeException"));
        verify(acknowledgment).acknowledge();
        verify(acknowledgment, never()).nack(anyLong());
        assertNotNull(consumer.finallyException);
    }

    @Test
    public void onMessageShouldNackWhenRetryRecordSaveFailed() {
        KafkaConsumerInvoker invoker = new KafkaConsumerInvoker();
        MessagePayloadSerializer serializer = mock(MessagePayloadSerializer.class);
        ConsumeIdempotentService consumeIdempotentService = mock(ConsumeIdempotentService.class);
        MessagePublishService messagePublishService = mock(MessagePublishService.class);
        RollbackConsumer consumer = new RollbackConsumer();
        KafkaConsumerMessageListener listener = new KafkaConsumerMessageListener(consumer, invoker, serializer,
                consumeIdempotentService, new TxnMqTransactionalService(), messagePublishService);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<String, byte[]> record = this.buildRecord("msg-6", "key-6", null, "root-6",
                KafkaPayloadCodec.gzip("\"payload-6\""));

        when(consumeIdempotentService.recordIfAbsent(any(ConsumeContext.class))).thenReturn(true);
        when(serializer.deserialize("\"payload-6\"", (Type) String.class)).thenReturn("payload-6");
        when(messagePublishService.scheduleConsumeRetry(eq(MqType.KAFKA), any(TransactionalMessage.class),
                any(ConsumeContext.class), eq(Duration.ofMinutes(2L)), anyString()))
                .thenThrow(new IllegalStateException("retry save failed"));

        listener.onMessage(record, acknowledgment);

        verify(acknowledgment).nack(Duration.ofMillis(10000L));
        verify(acknowledgment, never()).acknowledge();
    }

    private ConsumerRecord<String, byte[]> buildRecord(String messageId, String messageKey, String parentId,
                                                       String rootId, byte[] payload) {
        RecordHeaders headers = new RecordHeaders();
        headers.add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
        headers.add("messageKey", messageKey.getBytes(StandardCharsets.UTF_8));
        headers.add("traceId", "trace-1".getBytes(StandardCharsets.UTF_8));
        headers.add("contentEncoding", "gzip".getBytes(StandardCharsets.UTF_8));
        if (parentId != null) {
            headers.add("parentId", parentId.getBytes(StandardCharsets.UTF_8));
        }
        if (rootId != null) {
            headers.add("rootId", rootId.getBytes(StandardCharsets.UTF_8));
        }
        return new ConsumerRecord<String, byte[]>("topic.demo", 0, 0L, 0L, TimestampType.CREATE_TIME,
                null, 0, 0, messageKey, payload, headers, Optional.<Integer>empty());
    }

    private static final class RecordingConsumer implements TransactionalMessageConsumer<String> {

        private Exception finallyException;

        @Override
        public String getQueueName() {
            return "topic.listener";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.KAFKA;
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

    private static final class ExceptionConsumer implements TransactionalMessageConsumer<String> {

        private ConsumeHandleContext handleContext;
        private Exception finallyException;

        @Override
        public String getQueueName() {
            return "topic.exception";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.KAFKA;
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

    private static final class RollbackConsumer implements TransactionalMessageConsumer<String> {

        private Exception finallyException;

        @Override
        public String getQueueName() {
            return "topic.demo";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.KAFKA;
        }

        @Override
        public String consumerCode() {
            return "consumer-rollback";
        }

        @Override
        public ConsumeRetryPolicy getConsumeRetryPolicy() {
            return ConsumeRetryPolicy.fixedDelay(2, Duration.ofMinutes(2L));
        }

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            handleContext.setRollBack(true)
                    .addFinallyCall(exception -> this.finallyException = exception);
        }
    }

    private abstract static class AbstractParentConsumer<T extends BaseMessage<T>> implements TransactionalMessageConsumer<T> {

        @Override
        public String getQueueName() {
            return "topic.abstract";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.KAFKA;
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
