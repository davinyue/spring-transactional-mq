package org.rdlinux.transactionalmq.core.service.impl;

import org.rdlinux.transactionalmq.core.service.MessageDispatchWakeupService;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 派发线程唤醒协调器。
 */
public class MessageDispatchWakeupCoordinator implements MessageDispatchWakeupService {

    private final Semaphore wakeupSignal = new Semaphore(0);

    @Override
    public void wakeup() {
        this.wakeupSignal.release();
    }

    /**
     * 等待下一次唤醒或超时。
     *
     * @param timeoutMillis 超时时间
     */
    @Override
    public void await(long timeoutMillis) {
        long safeTimeoutMillis = timeoutMillis < 1 ? 1000L : timeoutMillis;

        try {
            boolean acquired = this.wakeupSignal.tryAcquire(safeTimeoutMillis, TimeUnit.MILLISECONDS);
            if (acquired) {
                // 已经消费了 1 个许可证，再清空剩余累计的许可证
                this.wakeupSignal.drainPermits();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待派发线程唤醒时被中断", e);
        }
    }
}