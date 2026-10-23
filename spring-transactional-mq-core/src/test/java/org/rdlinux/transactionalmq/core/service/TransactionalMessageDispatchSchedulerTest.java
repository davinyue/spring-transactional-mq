package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.rdlinux.transactionalmq.core.service.impl.MessageDispatchWakeupCoordinator;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 事务消息派发调度器测试
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

    /**
     * 验证调度线程按最近消息的剩余延迟时间等待。
     *
     * @throws Exception 等待后台线程进入休眠失败
     */
    @Test
    public void idleLoopShouldWaitUntilNextDispatchTime() throws Exception {
        CapturingMessageDispatchService messageDispatchService = new CapturingMessageDispatchService();
        messageDispatchService.dispatchResult = 0;
        messageDispatchService.nextDispatchTime = new Date(System.currentTimeMillis() + 15000L);
        long requestedWaitMillis = messageDispatchService.nextDispatchTime.getTime() - System.currentTimeMillis();
        CapturingWakeupService wakeupService = new CapturingWakeupService();
        TransactionalMessageDispatchScheduler scheduler = new TransactionalMessageDispatchScheduler(
                messageDispatchService, 16, 30000L, wakeupService);

        try {
            scheduler.afterPropertiesSet();

            Assert.assertTrue(wakeupService.awaitStarted.await(5L, TimeUnit.SECONDS));
            Assert.assertTrue(wakeupService.timeoutMillis > 0L);
            Assert.assertTrue(wakeupService.timeoutMillis < 30000L);
            Assert.assertTrue(wakeupService.timeoutMillis <= requestedWaitMillis);
        } finally {
            scheduler.destroy();
        }
    }

    /**
     * 验证到期时间较远时等待时长受配置的空闲上限约束。
     *
     * @throws Exception 等待后台线程进入休眠失败
     */
    @Test
    public void idleLoopShouldCapWaitAtConfiguredIdleInterval() throws Exception {
        CapturingMessageDispatchService messageDispatchService = new CapturingMessageDispatchService();
        messageDispatchService.dispatchResult = 0;
        messageDispatchService.nextDispatchTime = new Date(System.currentTimeMillis() + 120000L);
        CapturingWakeupService wakeupService = new CapturingWakeupService();
        TransactionalMessageDispatchScheduler scheduler = new TransactionalMessageDispatchScheduler(
                messageDispatchService, 16, 2000L, wakeupService);

        try {
            scheduler.afterPropertiesSet();

            Assert.assertTrue(wakeupService.awaitStarted.await(5L, TimeUnit.SECONDS));
            Assert.assertEquals(2000L, wakeupService.timeoutMillis);
        } finally {
            scheduler.destroy();
        }
    }

    /**
     * 验证没有未来待派发消息时仍使用配置的空闲等待上限。
     *
     * @throws Exception 等待后台线程进入休眠失败
     */
    @Test
    public void idleLoopShouldUseConfiguredIntervalWhenNoNextDispatchTime() throws Exception {
        CapturingMessageDispatchService messageDispatchService = new CapturingMessageDispatchService();
        messageDispatchService.dispatchResult = 0;
        CapturingWakeupService wakeupService = new CapturingWakeupService();
        TransactionalMessageDispatchScheduler scheduler = new TransactionalMessageDispatchScheduler(
                messageDispatchService, 16, 2345L, wakeupService);

        try {
            scheduler.afterPropertiesSet();

            Assert.assertTrue(wakeupService.awaitStarted.await(5L, TimeUnit.SECONDS));
            Assert.assertEquals(2345L, wakeupService.timeoutMillis);
        } finally {
            scheduler.destroy();
        }
    }

    private static class CapturingMessageDispatchService extends MessageDispatchService {

        private int lastBatchSize;
        /**
         * 模拟本轮派发数量。
         */
        private int dispatchResult = 3;
        /**
         * 模拟仓储返回的最近派发时间。
         */
        private Date nextDispatchTime;

        private CapturingMessageDispatchService() {
            super(new NoopTransactionalMessageRepository(),
                    new MqProducerRouter(Collections.<MqProducerAdapter>singletonList(new NoopMqProducerAdapter())),
                    new NoopMessageSendLogRepository());
        }

        @Override
        public int dispatchPendingMessages(int limit) {
            this.lastBatchSize = limit;
            return this.dispatchResult;
        }

        @Override
        public Date findNextDispatchTime() {
            return this.nextDispatchTime;
        }
    }

    /**
     * 捕获调度器等待时长并允许测试结束时释放后台线程。
     */
    private static class CapturingWakeupService implements MessageDispatchWakeupService {

        /**
         * 后台线程进入等待时释放的信号。
         */
        private final CountDownLatch awaitStarted = new CountDownLatch(1);
        /**
         * 测试结束时释放后台线程的信号。
         */
        private final CountDownLatch releaseAwait = new CountDownLatch(1);
        /**
         * 捕获到的等待时长。
         */
        private volatile long timeoutMillis;

        @Override
        public void wakeup() {
            this.releaseAwait.countDown();
        }

        @Override
        public void await(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            this.awaitStarted.countDown();
            try {
                this.releaseAwait.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
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
        public MqType supportMqType() {
            return MqType.RABBITMQ;
        }

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
