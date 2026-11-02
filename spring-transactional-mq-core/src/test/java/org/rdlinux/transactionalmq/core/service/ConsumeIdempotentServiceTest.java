package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 消费幂等服务测试
 */
public class ConsumeIdempotentServiceTest {

    /** 验证 GaussDB 重复键只在消息和消费者匹配时被视为已消费。 */
    @Test
    public void duplicateSqlStateRequiresMatchingConsumedRecord() {
        CapturingConsumedMessageRepository repository = new CapturingConsumedMessageRepository(true);
        repository.failure = new DataIntegrityViolationException("duplicate", new SQLException("duplicate", "23505"));
        repository.exists = true;
        ConsumeIdempotentService service = new ConsumeIdempotentService(repository);
        ConsumeContext context = new ConsumeContext().setId("retry").setOriginalMessageId("original")
                .setConsumerCode("consumer");
        Assert.assertFalse(service.recordIfAbsent(context));
        Assert.assertEquals("original", repository.savedRecord.getId());
        repository.exists = false;
        try {
            service.recordIfAbsent(context);
            Assert.fail("未匹配的冲突必须抛出");
        } catch (DataIntegrityViolationException ex) {
            Assert.assertSame(repository.failure, ex);
        }
        repository.exists = true;
        repository.failure = new DataIntegrityViolationException("other", new SQLException("not null", "23502"));
        try {
            service.recordIfAbsent(context);
            Assert.fail("其他完整性异常必须抛出");
        } catch (DataIntegrityViolationException ex) {
            Assert.assertSame(repository.failure, ex);
        }
    }

    @Test
    public void recordIfAbsentShouldUseAtomicSaveIfAbsent() {
        CapturingConsumedMessageRepository repository = new CapturingConsumedMessageRepository(true);
        ConsumeIdempotentService service = new ConsumeIdempotentService(repository);

        boolean recorded = service.recordIfAbsent(new ConsumeContext()
                .setId("msg-1")
                .setMessageKey("message-key-1")
                .setConsumerCode("consumer-1"));

        Assert.assertTrue(recorded);
        Assert.assertEquals(1, repository.saveIfAbsentCalls);
        Assert.assertEquals("msg-1", repository.savedRecord.getId());
        Assert.assertEquals("consumer-1", repository.savedRecord.getConsumerCode());
        Assert.assertEquals(org.rdlinux.transactionalmq.common.enums.ConsumeStatus.SUCCESS,
                repository.savedRecord.getConsumeStatus());
    }

    @Test
    public void recordIfAbsentShouldReturnFalseWhenAtomicSaveRejectsDuplicate() {
        CapturingConsumedMessageRepository repository = new CapturingConsumedMessageRepository(false);
        ConsumeIdempotentService service = new ConsumeIdempotentService(repository);

        boolean recorded = service.recordIfAbsent(new ConsumeContext()
                .setId("msg-2")
                .setMessageKey("message-key-2")
                .setConsumerCode("consumer-2"));

        Assert.assertFalse(recorded);
        Assert.assertEquals(1, repository.saveIfAbsentCalls);
    }

    /**
     * 验证消费幂等使用原始消息 id
     */
    @Test
    public void recordIfAbsentShouldUseOriginalMessageId() {
        CapturingConsumedMessageRepository repository = new CapturingConsumedMessageRepository(true);
        ConsumeIdempotentService service = new ConsumeIdempotentService(repository);

        service.recordIfAbsent(new ConsumeContext()
                .setId("retry-message-1")
                .setOriginalMessageId("original-message-1")
                .setRetryCount(1)
                .setMessageKey("message-key-retry")
                .setConsumerCode("consumer-retry"));

        Assert.assertEquals("original-message-1", repository.savedRecord.getId());
    }

    @Test
    public void consumedMessageRecordFromShouldDefaultToSuccessForArchiveFlow() {
        ConsumedMessageRecord record = ConsumedMessageRecord.from(new ConsumeContext()
                .setId("msg-3")
                .setMessageKey("message-key-3")
                .setConsumerCode("consumer-3"));

        Assert.assertEquals(org.rdlinux.transactionalmq.common.enums.ConsumeStatus.SUCCESS, record.getConsumeStatus());
        Assert.assertEquals("msg-3", record.getId());
    }

    private static class CapturingConsumedMessageRepository implements ConsumedMessageRepository {

        private final boolean saveResult;
        private int saveIfAbsentCalls;
        private ConsumedMessageRecord savedRecord;
        /** 模拟持久化异常。 */
        private DataIntegrityViolationException failure;
        /** 是否存在相同消费记录。 */
        private boolean exists;

        private CapturingConsumedMessageRepository(boolean saveResult) {
            this.saveResult = saveResult;
        }

        @Override
        public boolean saveIfAbsent(ConsumedMessageRecord record) {
            this.saveIfAbsentCalls++;
            this.savedRecord = record;
            if (this.failure != null) {
                throw this.failure;
            }
            return this.saveResult;
        }

        @Override
        public boolean exists(ConsumedMessageRecord record) {
            return this.exists;
        }

        @Override
        public List<ConsumedMessageRecord> findArchiveCandidates(Date archiveBefore, int limit) {
            return Collections.emptyList();
        }

        @Override
        public int archive(List<ConsumedMessageRecord> records) {
            return 0;
        }
    }
}
