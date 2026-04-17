package org.rdlinux.transactionalmq.core.service;

import java.util.Date;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;

/**
 * 消费幂等服务测试
 */
public class ConsumeIdempotentServiceTest {

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

        private CapturingConsumedMessageRepository(boolean saveResult) {
            this.saveResult = saveResult;
        }

        @Override
        public boolean saveIfAbsent(ConsumedMessageRecord record) {
            this.saveIfAbsentCalls++;
            this.savedRecord = record;
            return this.saveResult;
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
