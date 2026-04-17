package org.rdlinux.transactionalmq.core.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 事务消息后台派发线程
 *
 * <p>该线程持续轮询数据库中的待发送消息当没有候选消息时会进入休眠；
 * 发送服务在事务提交成功后会通过 {@link MessageDispatchWakeupService#wakeup()} 提前唤醒它，
 * 减少新消息入库后的等待时间</p>
 */
public class TransactionalMessageDispatchScheduler implements InitializingBean, DisposableBean {
    /**
     * 消息派发服务
     */
    private final MessageDispatchService messageDispatchService;
    /**
     * 单次派发批量大小
     */
    private final int dispatchBatchSize;
    /**
     * 空闲睡眠毫秒数
     */
    private final long dispatchIdleSleepMillis;
    /**
     * 派发线程唤醒器
     */
    private final MessageDispatchWakeupService messageDispatchWakeupService;

    private volatile boolean running = true;
    private Thread workerThread;

    /**
     * 构造后台派发线程
     *
     * @param messageDispatchService       消息派发服务
     * @param dispatchBatchSize            单次派发批量大小
     * @param dispatchIdleSleepMillis      空闲睡眠毫秒数
     * @param messageDispatchWakeupService 派发线程唤醒器
     */
    public TransactionalMessageDispatchScheduler(MessageDispatchService messageDispatchService,
                                                 int dispatchBatchSize, long dispatchIdleSleepMillis,
                                                 MessageDispatchWakeupService messageDispatchWakeupService) {
        this.messageDispatchService = messageDispatchService;
        this.dispatchBatchSize = dispatchBatchSize;
        this.dispatchIdleSleepMillis = dispatchIdleSleepMillis;
        this.messageDispatchWakeupService = messageDispatchWakeupService;
    }

    @Override
    public void afterPropertiesSet() {
        this.workerThread = new Thread(this::runLoop, "txn-mq-dispatch-worker");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    private void runLoop() {
        while (this.running) {
            try {
                int dispatched = this.dispatchNow();
                if (dispatched > 0) {
                    continue;
                }
                this.waitForNextRound(this.dispatchIdleSleepMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable ex) {
                try {
                    this.waitForNextRound(this.dispatchIdleSleepMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * 立即执行一轮派发
     *
     * @return 本轮派发数量
     */
    public int dispatchNow() {
        return this.messageDispatchService.dispatchPendingMessages(this.dispatchBatchSize);
    }

    private void waitForNextRound(long sleepMillis) throws InterruptedException {
        this.messageDispatchWakeupService.await(sleepMillis);
    }

    @Override
    public void destroy() throws Exception {
        this.running = false;
        this.messageDispatchWakeupService.wakeup();
        if (this.workerThread != null) {
            this.workerThread.interrupt();
            this.workerThread.join(1000L);
        }
    }
}
