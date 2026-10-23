package org.rdlinux.transactionalmq.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;

/**
 * 事务消息后台派发线程
 *
 * <p>该线程持续轮询数据库中的待发送消息；没有到期候选时会按最近的未来派发时间休眠，
 * 且休眠不超过最大空闲间隔。发送服务在事务提交成功后会通过
 * {@link MessageDispatchWakeupService#wakeup()} 提前唤醒它</p>
 */
@Slf4j
public class TransactionalMessageDispatchScheduler implements InitializingBean, DisposableBean {
    /**
     * 遇到已经到期但暂时未被领取的消息时，最短休眠时间（毫秒）
     */
    private static final long MIN_IDLE_RETRY_SLEEP_MILLIS = 100L;

    /**
     * 消息派发服务
     */
    private final MessageDispatchService messageDispatchService;
    /**
     * 单次派发批量大小
     */
    private final int dispatchBatchSize;
    /**
     * 最大空闲睡眠毫秒数
     */
    private final long dispatchIdleSleepMillis;
    /**
     * 派发线程唤醒器
     */
    private final MessageDispatchWakeupService messageDispatchWakeupService;

    /**
     * 是否继续运行派发线程
     */
    private volatile boolean running = true;
    /**
     * 后台派发工作线程
     */
    private Thread workerThread;

    /**
     * 构造后台派发线程
     *
     * @param messageDispatchService       消息派发服务
     * @param dispatchBatchSize            单次派发批量大小
     * @param dispatchIdleSleepMillis      最大空闲睡眠毫秒数
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

    /**
     * 执行后台派发循环
     */
    private void runLoop() {
        while (this.running) {
            try {
                int dispatched = this.dispatchNow();
                if (dispatched > 0) {
                    continue;
                }
                this.waitForNextRound(this.calculateIdleSleepMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable ex) {
                log.error("事务消息后台派发异常，将在 {} ms 后重试", this.dispatchIdleSleepMillis, ex);
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

    /**
     * 计算下一轮派发前的休眠时间
     *
     * @return 休眠毫秒数
     */
    private long calculateIdleSleepMillis() {
        long maxSleepMillis = this.dispatchIdleSleepMillis < 1L ? 1000L : this.dispatchIdleSleepMillis;
        Date nextDispatchTime = this.messageDispatchService.findNextDispatchTime();
        if (nextDispatchTime == null) {
            return maxSleepMillis;
        }
        long delayMillis = nextDispatchTime.getTime() - System.currentTimeMillis();
        if (delayMillis <= 0L) {
            return Math.min(maxSleepMillis, MIN_IDLE_RETRY_SLEEP_MILLIS);
        }
        return Math.min(maxSleepMillis, delayMillis);
    }

    /**
     * 等待下一轮派发或被提前唤醒
     *
     * @param sleepMillis 最大等待毫秒数
     * @throws InterruptedException 等待过程被中断
     */
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
