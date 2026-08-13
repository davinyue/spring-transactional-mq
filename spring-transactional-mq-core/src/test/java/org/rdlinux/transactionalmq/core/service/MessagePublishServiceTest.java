package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 消息发布服务测试
 */
public class MessagePublishServiceTest {
    @Test
    public void publishShouldSaveRecordAndReturnAcceptedResult() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePayloadSerializer serializer = new MessagePayloadSerializer() {
            @Override
            public String serialize(Object payload) {
                return "serialized-" + payload;
            }

            @Override
            public <T> T deserialize(String payloadText, Type targetType) {
                return null;
            }
        };
        MessagePublishService service = new MessagePublishService(repository, serializer, null,
                this.buildRouter(MqType.RABBITMQ));

        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setId("msg-1")
                .setMessageKey("message-key-1")
                .setProducerCode("producer-1")
                .setDestination("demo.exchange")
                .setRoute("demo.route")
                .setShardingKey("order-1")
                .setPayload("payload-value")
                .setBizKey("biz-1");

        String messageId = service.send(MqType.RABBITMQ, message);

        Assert.assertNotNull(messageId);
        Assert.assertNotNull(repository.savedRecord);
        Assert.assertEquals(messageId, repository.savedRecord.getId());
        Assert.assertEquals(messageId, repository.savedRecord.getRootId());
        Assert.assertEquals(messageId, repository.savedRecord.getOriginalMessageId());
        Assert.assertEquals(Integer.valueOf(0), repository.savedRecord.getRetryCount());
        Assert.assertNull(repository.savedRecord.getParentId());
        Assert.assertEquals("serialized-payload-value", repository.savedRecord.getPayloadText());
        Assert.assertEquals("producer-1", repository.savedRecord.getProducerCode());
        Assert.assertEquals("demo.exchange", repository.savedRecord.getDestination());
        Assert.assertEquals("demo.route", repository.savedRecord.getRoute());
        Assert.assertEquals("order-1", repository.savedRecord.getShardingKey());
    }

    @Test
    public void transactionalMessageRecordFromShouldNotGenerateSeparateMessageIdentifier() {
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setId("api-id")
                .setMessageKey("message-key-2")
                .setProducerCode("producer-2")
                .setDestination("topic-demo")
                .setRoute("tag-a")
                .setShardingKey("order-2")
                .setPayload("payload-value");

        TransactionalMessageRecord record = TransactionalMessageRecord.from(MqType.RABBITMQ, message, "serialized");

        Assert.assertNull(record.getId());
        Assert.assertEquals("message-key-2", record.getMessageKey());
        Assert.assertEquals("topic-demo", record.getDestination());
        Assert.assertEquals("tag-a", record.getRoute());
        Assert.assertEquals("order-2", record.getShardingKey());
        Assert.assertEquals("serialized", record.getPayloadText());
        Assert.assertNull(record.getParentId());
        Assert.assertNull(record.getRootId());
    }

    @Test
    public void sendWithParentShouldPropagateParentAndRootIdentifiers() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePayloadSerializer serializer = new MessagePayloadSerializer() {
            @Override
            public String serialize(Object payload) {
                return "serialized-" + payload;
            }

            @Override
            public <T> T deserialize(String payloadText, Type targetType) {
                return null;
            }
        };
        MessagePublishService service = new MessagePublishService(repository, serializer, null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-3")
                .setPayload("payload-value-3");
        ConsumeContext parentContext = new ConsumeContext()
                .setId("parent-1")
                .setRootId("root-1")
                .setMessageKey("message-key-parent")
                .setConsumerCode("consumer-1");

        String childId = service.sendWithParent(MqType.RABBITMQ, message, parentContext);

        Assert.assertNotNull(childId);
        Assert.assertEquals(childId, repository.savedRecord.getId());
        Assert.assertEquals("parent-1", repository.savedRecord.getParentId());
        Assert.assertEquals("root-1", repository.savedRecord.getRootId());
    }

    @Test
    public void sendShouldBeTransactional() throws Exception {
        Method method = MessagePublishService.class.getMethod("send", MqType.class, TransactionalMessage.class);

        Assert.assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    public void sendShouldFailBeforeSaveWhenMqTypeUnsupported() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePayloadSerializer serializer = new MessagePayloadSerializer() {
            @Override
            public String serialize(Object payload) {
                return "serialized-" + payload;
            }

            @Override
            public <T> T deserialize(String payloadText, Type targetType) {
                return null;
            }
        };
        MessagePublishService service = new MessagePublishService(repository, serializer, null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-4")
                .setPayload("payload-value-4");

        try {
            service.send(MqType.KAFKA, message);
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("unsupported mqType: KAFKA", ex.getMessage());
        }

        Assert.assertNull(repository.savedRecord);
    }

    /**
     * 验证消费重试记录生成新 id 并保留原始消息 id 和链路
     */
    @Test
    public void scheduleConsumeRetryShouldPreserveOriginalMessageAndIncreaseRetryCount() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.KAFKA));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-retry")
                .setProducerCode("producer-retry")
                .setDestination("topic.retry")
                .setPayload("payload-retry");
        ConsumeContext context = new ConsumeContext()
                .setId("attempt-2")
                .setOriginalMessageId("original-1")
                .setRetryCount(2)
                .setParentId("parent-1")
                .setRootId("root-1")
                .setConsumerCode("consumer-retry");

        boolean saved = service.scheduleConsumeRetry(MqType.KAFKA, message, context,
                Duration.ofMinutes(4), "business failed");

        Assert.assertTrue(saved);
        Assert.assertNotEquals("attempt-2", repository.retryRecord.getId());
        Assert.assertEquals("original-1", repository.retryRecord.getOriginalMessageId());
        Assert.assertEquals(Integer.valueOf(3), repository.retryRecord.getRetryCount());
        Assert.assertEquals("parent-1", repository.retryRecord.getParentId());
        Assert.assertEquals("root-1", repository.retryRecord.getRootId());
        Assert.assertEquals("consumer-retry", repository.retryRecord.getConsumerCode());
        Assert.assertEquals("business failed", repository.retryRecord.getLastError());
        Assert.assertTrue(repository.retryRecord.getNextDispatchTime().after(new Date()));
    }

    /**
     * 验证停止重试时保存不可派发审计记录
     */
    @Test
    public void recordConsumeRetryStoppedShouldSaveDeadRecord() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-dead")
                .setDestination("queue.dead")
                .setPayload("payload-dead");
        ConsumeContext context = new ConsumeContext()
                .setId("attempt-dead")
                .setOriginalMessageId("original-dead")
                .setRetryCount(0)
                .setConsumerCode("consumer-dead");

        service.recordConsumeRetryStopped(MqType.RABBITMQ, message, context, "stop retry");

        Assert.assertEquals(org.rdlinux.transactionalmq.common.enums.MessageStatus.DEAD,
                repository.retryRecord.getMessageStatus());
        Assert.assertNull(repository.retryRecord.getNextDispatchTime());
        Assert.assertEquals(Integer.valueOf(1), repository.retryRecord.getRetryCount());
    }

    /**
     * 验证唯一重试键冲突按该轮记录已存在处理。
     */
    @Test
    public void scheduleConsumeRetryShouldTreatDuplicateKeyAsAlreadySaved() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        repository.duplicateConsumeRetry = true;
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.KAFKA));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setDestination("topic.retry")
                .setPayload("payload-retry");
        ConsumeContext context = new ConsumeContext()
                .setId("attempt-duplicate")
                .setOriginalMessageId("original-duplicate")
                .setRetryCount(1);

        boolean saved = service.scheduleConsumeRetry(MqType.KAFKA, message, context,
                Duration.ofMinutes(1L), "business failed");

        Assert.assertFalse(saved);
    }

    /**
     * 构建测试序列化器
     *
     * @return 测试序列化器
     */
    private MessagePayloadSerializer buildSerializer() {
        return new MessagePayloadSerializer() {
            @Override
            public String serialize(Object payload) {
                return "serialized-" + payload;
            }

            @Override
            public <T> T deserialize(String payloadText, Type targetType) {
                return null;
            }
        };
    }

    private MqProducerRouter buildRouter(MqType mqType) {
        return new MqProducerRouter(Collections.<MqProducerAdapter>singletonList(new MqProducerAdapter() {
            @Override
            public MqType supportMqType() {
                return mqType;
            }

            @Override
            public void send(org.rdlinux.transactionalmq.core.model.DispatchMessage message) {
            }
        }));
    }

    private static class CapturingTransactionalMessageRepository implements TransactionalMessageRepository {

        private TransactionalMessageRecord savedRecord;
        private TransactionalMessageRecord retryRecord;
        private boolean duplicateConsumeRetry;

        @Override
        public TransactionalMessageRecord save(TransactionalMessageRecord record) {
            this.savedRecord = record;
            return record;
        }

        @Override
        public void saveConsumeRetry(TransactionalMessageRecord record) {
            if (this.duplicateConsumeRetry) {
                throw new DuplicateKeyException("duplicate consume retry");
            }
            this.retryRecord = record;
        }

        @Override
        public List<TransactionalMessageRecord> findDispatchCandidates(int limit) {
            return Collections.emptyList();
        }

        @Override
        public TransactionalMessageRecord claimDispatchMessage(TransactionalMessageRecord record) {
            return null;
        }

        @Override
        public void markDispatchSuccess(List<TransactionalMessageRecord> records) {
        }

        @Override
        public void markDispatchFailed(List<TransactionalMessageRecord> records) {
        }

        @Override
        public List<TransactionalMessageRecord> findSuccessCleanupCandidates(Date cleanupBefore, int limit) {
            return Collections.emptyList();
        }

        @Override
        public int archiveSuccessMessage(TransactionalMessageRecord record, Date cleanupBefore) {
            return 0;
        }
    }
}
