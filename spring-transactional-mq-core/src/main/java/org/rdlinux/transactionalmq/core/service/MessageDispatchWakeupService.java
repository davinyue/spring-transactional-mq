package org.rdlinux.transactionalmq.core.service;

/**
 * 消息派发唤醒器
 *
 * <p>发送服务在事务提交成功后通过该接口通知后台派发线程提前唤醒，
 * 避免消息已入库但派发线程仍处于空闲睡眠状态</p>
 */
public interface MessageDispatchWakeupService {

    /**
     * 唤醒后台派发线程
     */
    void wakeup();

    /**
     * 等待下一次唤醒或超时
     *
     * @param timeoutMillis 超时时间
     */
    void await(long timeoutMillis);
}
