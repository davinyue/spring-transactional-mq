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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.SQLException;
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

    /**
     * 验证延迟发送会保存最早派发时间并返回消息 id。
     */
    @Test
    public void sendAfterShouldSaveNextDispatchTimeAndReturnMessageId() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-delay")
                .setPayload("payload-delay");
        Duration delay = Duration.ofSeconds(5L);
        long requestTimeMillis = System.currentTimeMillis();

        String messageId = service.sendAfter(MqType.RABBITMQ, message, delay);

        long completionTimeMillis = System.currentTimeMillis();
        Assert.assertNotNull(messageId);
        Assert.assertEquals(messageId, repository.savedRecord.getId());
        Assert.assertTrue(repository.savedRecord.getNextDispatchTime().getTime()
                >= requestTimeMillis + delay.toMillis());
        Assert.assertTrue(repository.savedRecord.getNextDispatchTime().getTime()
                <= completionTimeMillis + delay.toMillis());
    }

    /**
     * 验证延迟发送会拒绝无效、精度不足和超出可表示范围的延迟。
     */
    @Test
    public void sendAfterShouldRejectInvalidDelayBeforeSaving() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-invalid-delay")
                .setPayload("payload-invalid-delay");
        Duration[] invalidDelays = new Duration[]{null, Duration.ZERO, Duration.ofMillis(-1L),
                Duration.ofNanos(1L), Duration.ofSeconds(Long.MAX_VALUE)};

        for (Duration delay : invalidDelays) {
            try {
                service.sendAfter(MqType.RABBITMQ, message, delay);
                Assert.fail("expected IllegalArgumentException for delay: " + delay);
            } catch (IllegalArgumentException expected) {
                Assert.assertNull(repository.savedRecord);
            }
        }
    }

    /**
     * 验证带父消息的延迟发送会保存链路标识和最早派发时间。
     */
    @Test
    public void sendWithParentAfterShouldSaveParentChainAndNextDispatchTime() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-parent-delay")
                .setPayload("payload-parent-delay");
        ConsumeContext parentContext = new ConsumeContext()
                .setId("parent-delay-1")
                .setRootId("root-delay-1");
        Duration delay = Duration.ofSeconds(4L);
        long requestTimeMillis = System.currentTimeMillis();

        String messageId = service.sendWithParentAfter(MqType.RABBITMQ, message, parentContext, delay);

        long completionTimeMillis = System.currentTimeMillis();
        Assert.assertEquals("parent-delay-1", repository.savedRecord.getParentId());
        Assert.assertEquals("root-delay-1", repository.savedRecord.getRootId());
        Assert.assertEquals(messageId, repository.savedRecord.getId());
        Assert.assertTrue(repository.savedRecord.getNextDispatchTime().getTime()
                >= requestTimeMillis + delay.toMillis());
        Assert.assertTrue(repository.savedRecord.getNextDispatchTime().getTime()
                <= completionTimeMillis + delay.toMillis());
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
     * 验证停止重试时直接保存死信历史记录。
     */
    @Test
    public void recordConsumeRetryStoppedShouldSaveDeadRecordDirectlyToHistory() {
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
                repository.deadRetryRecord.getMessageStatus());
        Assert.assertNull(repository.deadRetryRecord.getNextDispatchTime());
        Assert.assertEquals(Integer.valueOf(1), repository.deadRetryRecord.getRetryCount());
        Assert.assertNull(repository.retryRecord);
        Assert.assertNull(repository.savedRecord);
    }

    /**
     * 验证死信历史唯一键冲突按已经归档处理。
     */
    @Test
    public void recordConsumeRetryStoppedShouldIgnoreDuplicateHistoryRecord() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        repository.duplicateDeadConsumeRetry = true;
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setDestination("queue.dead")
                .setPayload("payload-dead");
        ConsumeContext context = new ConsumeContext()
                .setId("attempt-dead-duplicate")
                .setOriginalMessageId("original-dead-duplicate")
                .setRetryCount(2);

        service.recordConsumeRetryStopped(MqType.RABBITMQ, message, context, "stop retry");

        Assert.assertNull(repository.savedRecord);
        Assert.assertNull(repository.retryRecord);
        Assert.assertNull(repository.deadRetryRecord);
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
     * 验证高斯通用完整性异常中的唯一键 SQLState 在重试和死信路径均可去重。
     */
    @Test
    public void consumeRetryShouldIgnoreUniqueSqlState() {
        this.assertConsumeRetryFailure(new DataIntegrityViolationException("gauss unique violation",
                new SQLException("unique violation", "23505")), true);
    }

    /**
     * 验证嵌套原因链中的唯一键异常可被识别。
     */
    @Test
    public void consumeRetryShouldRecognizeNestedUniqueSqlState() {
        this.assertConsumeRetryFailure(new DataIntegrityViolationException("wrapped",
                new IllegalStateException(new SQLException("unique violation", "23505"))), true);
    }

    /**
     * 验证 JDBC nextException 链中的唯一键异常可被识别。
     */
    @Test
    public void consumeRetryShouldRecognizeChainedUniqueSqlState() {
        SQLException batchException = new SQLException("batch failed");
        batchException.setNextException(new SQLException("unique violation", "23505"));
        this.assertConsumeRetryFailure(new DataIntegrityViolationException("batch failed", batchException), true);
    }

    /**
     * 验证其他完整性错误原样抛出，不能误确认当前消息。
     */
    @Test
    public void consumeRetryShouldPropagateOtherIntegrityViolations() {
        for (String sqlState : new String[]{"23502", "23503", "23514", "23000", "22001", null}) {
            this.assertConsumeRetryFailure(new DataIntegrityViolationException("integrity violation",
                    new SQLException("integrity violation", sqlState)), false);
        }
    }

    /**
     * 验证异常文案不能作为唯一键冲突依据。
     */
    @Test
    public void consumeRetryShouldNotInferDuplicateFromErrorText() {
        this.assertConsumeRetryFailure(new DataIntegrityViolationException(
                "Duplicate key value violates unique constraint uk_txn_message_original_retry"), false);
    }

    /**
     * 检查两条重试保存路径对数据库异常的处理。
     *
     * @param failure 模拟数据库异常
     * @param duplicate 是否为可忽略的唯一键冲突
     */
    private void assertConsumeRetryFailure(DataIntegrityViolationException failure, boolean duplicate) {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        repository.consumeRetryFailure = failure;
        MessagePublishService service = new MessagePublishService(repository, this.buildSerializer(), null,
                this.buildRouter(MqType.RABBITMQ));
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setDestination("queue.retry").setPayload("payload-retry");
        ConsumeContext context = new ConsumeContext().setId("attempt-1").setOriginalMessageId("original-1");
        try {
            Assert.assertFalse(service.scheduleConsumeRetry(MqType.RABBITMQ, message, context,
                    Duration.ofMinutes(1), "business failed"));
            Assert.assertTrue("non-duplicate retry failure must propagate", duplicate);
        } catch (DataIntegrityViolationException ex) {
            Assert.assertFalse("duplicate retry failure must be ignored", duplicate);
            Assert.assertSame(failure, ex);
        }
        try {
            service.recordConsumeRetryStopped(MqType.RABBITMQ, message, context, "stop retry");
            Assert.assertTrue("non-duplicate history failure must propagate", duplicate);
        } catch (DataIntegrityViolationException ex) {
            Assert.assertFalse("duplicate history failure must be ignored", duplicate);
            Assert.assertSame(failure, ex);
        }
        Assert.assertNull(repository.retryRecord);
        Assert.assertNull(repository.deadRetryRecord);
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
        /**
         * 捕获的死信历史记录。
         */
        private TransactionalMessageRecord deadRetryRecord;
        private boolean duplicateConsumeRetry;
        /**
         * 是否模拟死信历史记录唯一键冲突。
         */
        private boolean duplicateDeadConsumeRetry;

        /**
         * 模拟重试主表和死信历史表的数据完整性异常。
         */
        private DataIntegrityViolationException consumeRetryFailure;

        @Override
        public TransactionalMessageRecord save(TransactionalMessageRecord record) {
            this.savedRecord = record;
            return record;
        }

        /**
         * 捕获消费重试记录，或模拟数据库保存异常。
         *
         * @param record 消费重试记录
         */
        @Override
        public void saveConsumeRetry(TransactionalMessageRecord record) {
            if (this.consumeRetryFailure != null) {
                throw this.consumeRetryFailure;
            }
            if (this.duplicateConsumeRetry) {
                throw new DuplicateKeyException("duplicate consume retry");
            }
            this.retryRecord = record;
        }

        /**
         * 捕获死信历史记录，或模拟唯一键冲突。
         *
         * @param record 死信记录
         */
        @Override
        public void saveDeadConsumeRetry(TransactionalMessageRecord record) {
            if (this.consumeRetryFailure != null) {
                throw this.consumeRetryFailure;
            }
            if (this.duplicateDeadConsumeRetry) {
                throw new DuplicateKeyException("duplicate dead consume retry");
            }
            this.deadRetryRecord = record;
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
