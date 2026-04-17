package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.rdlinux.transactionalmq.core.service.impl.MessageDispatchWakeupCoordinator;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 事务消息派发调度器测试。
 */
public class TransactionalMessageDispatchSchedulerTest {

    @Test
    public void dispatchNowShouldUseConfiguredBatchSize() {
        CapturingMessageDispatchService messageDispatchService = new CapturingMessageDispatchService();
        TransactionalMessageDispatchScheduler scheduler = new TransactionalMessageDispatchScheduler(
                messageDispatchService, 16, 1000L, new MessageDispatchWakeupCoordinator());

        int dispatched = scheduler.dispatchNow();

        Assert.assertEquals(3, dispatched);
        Assert.assertEquals(16, messageDispatchService.lastBatchSize);
    }

    private static class CapturingMessageDispatchService extends MessageDispatchService {

        private int lastBatchSize;

        private CapturingMessageDispatchService() {
            super(new NoopTransactionalMessageRepository(), new NoopMqProducerAdapter(),
                    new NoopMessageSendLogRepository());
        }

        @Override
        public int dispatchPendingMessages(int limit) {
            this.lastBatchSize = limit;
            return 3;
        }
    }

    private static class NoopTransactionalMessageRepository implements TransactionalMessageRepository {

        @Override
        public org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord save(
                org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord record) {
            return record;
        }

        @Override
        public List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> findDispatchCandidates(int limit) {
            return Collections.emptyList();
        }

        @Override
        public org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord claimDispatchMessage(
                org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord record) {
            return null;
        }

        @Override
        public void markDispatchSuccess(List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> records) {
        }

        @Override
        public void markDispatchFailed(List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> records) {
        }

        @Override
        public List<org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord> findSuccessCleanupCandidates(
                Date cleanupBefore, int limit) {
            return Collections.emptyList();
        }

        @Override
        public int archiveSuccessMessage(org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord record,
                                         Date cleanupBefore) {
            return 0;
        }
    }

    private static class NoopMqProducerAdapter implements org.rdlinux.transactionalmq.core.mq.MqProducerAdapter {

        @Override
        public void send(org.rdlinux.transactionalmq.core.model.DispatchMessage message) {
        }
    }

    private static class NoopMessageSendLogRepository implements org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository {

        @Override
        public org.rdlinux.transactionalmq.core.model.MessageSendLogRecord save(
                org.rdlinux.transactionalmq.core.model.MessageSendLogRecord record) {
            return record;
        }

        @Override
        public List<org.rdlinux.transactionalmq.core.model.MessageSendLogRecord> findRetryCandidates(int limit) {
            return Collections.emptyList();
        }
    }
}
