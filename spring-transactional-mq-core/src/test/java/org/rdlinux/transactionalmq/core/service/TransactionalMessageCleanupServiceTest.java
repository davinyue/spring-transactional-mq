package org.rdlinux.transactionalmq.core.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;

/**
 * 事务消息主表清理服务测试
 */
public class TransactionalMessageCleanupServiceTest {

    /**
     * 验证清理服务委托仓储删除已发送成功消息
     */
    @Test
    public void cleanupSuccessMessagesShouldDelegateToRepository() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        TransactionalMessageCleanupService service = new TransactionalMessageCleanupService(repository);
        Date cleanupBefore = new Date(1710000000000L);

        int deleted = service.cleanupSuccessMessages(cleanupBefore, 100);

        Assert.assertEquals(3, deleted);
        Assert.assertSame(cleanupBefore, repository.cleanupBefore);
        Assert.assertEquals(100, repository.limit);
        Assert.assertEquals(3, repository.archivedRecords.size());
        Assert.assertEquals("a-1", repository.archivedRecords.get(0).getId());
        Assert.assertEquals("b-1", repository.archivedRecords.get(1).getId());
        Assert.assertEquals("c-1", repository.archivedRecords.get(2).getId());
    }

    private static final class CapturingTransactionalMessageRepository implements TransactionalMessageRepository {

        private Date cleanupBefore;
        private int limit;
        private final List<TransactionalMessageRecord> archivedRecords = new java.util.ArrayList<TransactionalMessageRecord>();

        @Override
        public TransactionalMessageRecord save(TransactionalMessageRecord record) {
            return record;
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
            this.cleanupBefore = cleanupBefore;
            this.limit = limit;
            List<TransactionalMessageRecord> candidates = new java.util.ArrayList<TransactionalMessageRecord>();
            candidates.add(new TransactionalMessageRecord().setId("c-1"));
            candidates.add(new TransactionalMessageRecord().setId("a-1"));
            candidates.add(new TransactionalMessageRecord().setId("b-1"));
            return candidates;
        }

        @Override
        public int archiveSuccessMessage(TransactionalMessageRecord record, Date cleanupBefore) {
            this.archivedRecords.add(record);
            return 1;
        }
    }
}
