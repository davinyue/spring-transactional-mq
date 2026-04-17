package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 消费记录清理调度器测试
 */
public class ConsumedMessageCleanupSchedulerTest {

    @Test
    public void cleanupConsumedMessagesShouldUseConfiguredBatchSize() {
        CapturingConsumedMessageCleanupService cleanupService = new CapturingConsumedMessageCleanupService(3);
        ConsumedMessageCleanupScheduler scheduler = new ConsumedMessageCleanupScheduler(cleanupService, 7, 12);

        scheduler.cleanupConsumedMessages();

        Assert.assertEquals(1, cleanupService.callCount);
        Assert.assertEquals(12, cleanupService.lastLimit);
        Assert.assertNotNull(cleanupService.lastCleanupBefore);
    }

    @Test
    public void cleanupConsumedMessagesShouldContinueWhenCleanedReachedLimit() {
        CapturingConsumedMessageCleanupService cleanupService = new CapturingConsumedMessageCleanupService(5, 5, 2);
        ConsumedMessageCleanupScheduler scheduler = new ConsumedMessageCleanupScheduler(cleanupService, 7, 5);

        scheduler.cleanupConsumedMessages();

        Assert.assertEquals(3, cleanupService.callCount);
        Assert.assertEquals(5, cleanupService.lastLimit);
    }

    @Test
    public void cleanupConsumedMessagesShouldNotDependOnScheduledAnnotation() throws Exception {
        Method method = ConsumedMessageCleanupScheduler.class.getMethod("cleanupConsumedMessages");

        for (Annotation annotation : method.getAnnotations()) {
            Assert.assertNotEquals("org.springframework.scheduling.annotation.Scheduled",
                    annotation.annotationType().getName());
        }
    }

    private static class CapturingConsumedMessageCleanupService extends ConsumedMessageCleanupService {

        private final int[] cleanedResults;
        private int index;
        private int callCount;
        private Date lastCleanupBefore;
        private int lastLimit;

        private CapturingConsumedMessageCleanupService(int... cleanedResults) {
            super(new NoopConsumedMessageRepository());
            this.cleanedResults = cleanedResults.length == 0 ? new int[]{1} : cleanedResults;
        }

        @Override
        public int cleanupOlderThan(Date cleanupBefore, int limit) {
            this.callCount++;
            this.lastCleanupBefore = cleanupBefore;
            this.lastLimit = limit;
            int safeIndex = Math.min(this.index, this.cleanedResults.length - 1);
            this.index++;
            return this.cleanedResults[safeIndex];
        }
    }

    private static class NoopConsumedMessageRepository implements ConsumedMessageRepository {

        @Override
        public boolean saveIfAbsent(ConsumedMessageRecord record) {
            return true;
        }

        @Override
        public List<ConsumedMessageRecord> findArchiveCandidates(Date cleanupBefore, int limit) {
            return Collections.emptyList();
        }

        @Override
        public int archive(List<ConsumedMessageRecord> records) {
            return 0;
        }
    }
}
