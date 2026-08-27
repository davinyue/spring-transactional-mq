package org.rdlinux.transactionalmq.api.consumer;

/**
 * 消费者finally执行回调
 */
@FunctionalInterface
public interface FinallyCall {
    /**
     * 消费者finally执行回调
     *
     * @param e 当出现异常时 e 不为空, 否则 e 为空
     */
    void call(Exception e);
}
