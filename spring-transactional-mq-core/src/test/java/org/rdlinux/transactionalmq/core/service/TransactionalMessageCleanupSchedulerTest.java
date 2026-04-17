package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * 成功消息清理调度器测试
 */
public class TransactionalMessageCleanupSchedulerTest {

    @Test
    public void cleanupSuccessMessagesShouldUseConfiguredBatchSize() {
        CapturingTransactionalMessageCleanupService cleanupService = new CapturingTransactionalMessageCleanupService();
        TransactionalMessageCleanupScheduler scheduler = new TransactionalMessageCleanupScheduler(cleanupService, 7, 12);

        scheduler.cleanupSuccessMessages();

        Assert.assertEquals(1, cleanupService.callCount);
        Assert.assertEquals(12, cleanupService.lastLimit);
        Assert.assertNotNull(cleanupService.lastCleanupBefore);
    }

    @Test
    public void cleanupSuccessMessagesShouldContinueWhenCleanedReachedLimit() {
        CapturingTransactionalMessageCleanupService cleanupService = new CapturingTransactionalMessageCleanupService(5, 5, 2);
        TransactionalMessageCleanupScheduler scheduler = new TransactionalMessageCleanupScheduler(cleanupService, 7, 5);

        scheduler.cleanupSuccessMessages();

        Assert.assertEquals(3, cleanupService.callCount);
        Assert.assertEquals(5, cleanupService.lastLimit);
    }

    @Test
    public void cleanupSuccessMessagesShouldNotDependOnScheduledAnnotation() throws Exception {
        Method method = TransactionalMessageCleanupScheduler.class.getMethod("cleanupSuccessMessages");

        for (Annotation annotation : method.getAnnotations()) {
            Assert.assertNotEquals("org.springframework.scheduling.annotation.Scheduled",
                    annotation.annotationType().getName());
        }
    }

    private static class CapturingTransactionalMessageCleanupService extends TransactionalMessageCleanupService {

        private final int[] cleanedResults;
        private int index;
        private int callCount;
        private Date lastCleanupBefore;
        private int lastLimit;

        private CapturingTransactionalMessageCleanupService(int... cleanedResults) {
            super(new NoopTransactionalMessageRepository());
            this.cleanedResults = cleanedResults.length == 0 ? new int[]{3} : cleanedResults;
        }

        @Override
        public int cleanupSuccessMessages(Date cleanupBefore, int limit) {
            this.callCount++;
            this.lastCleanupBefore = cleanupBefore;
            this.lastLimit = limit;
            int safeIndex = Math.min(this.index, this.cleanedResults.length - 1);
            this.index++;
            return this.cleanedResults[safeIndex];
        }
    }

    private static class NoopTransactionalMessageRepository implements org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository {

        @Override
        public org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord save(
                org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord record) {
            return record;
        }

        @Override
        public java.util.List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> findDispatchCandidates(
                int limit) {
            return java.util.Collections.emptyList();
        }

        @Override
        public org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord claimDispatchMessage(
                org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord record) {
            return null;
        }

        @Override
        public void markDispatchSuccess(java.util.List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> records) {
        }

        @Override
        public void markDispatchFailed(java.util.List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> records) {
        }

        @Override
        public java.util.List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> findSuccessCleanupCandidates(
                Date cleanupBefore, int limit) {
            return java.util.Collections.emptyList();
        }

        @Override
        public int archiveSuccessMessage(org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord record,
                                         Date cleanupBefore) {
            return 0;
        }
    }
}
