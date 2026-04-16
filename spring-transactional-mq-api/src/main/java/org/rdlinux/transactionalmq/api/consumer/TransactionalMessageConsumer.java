package org.rdlinux.transactionalmq.api.consumer;

import org.rdlinux.transactionalmq.api.model.ConsumeContext;

/**
 * 统一事务消息消费者。
 *
 * @param <T> 消息负载类型
 */
public interface TransactionalMessageConsumer<T> {

    /**
     * 获取消费队列名。
     *
     * @return 队列名
     */
    String getQueueName();

    /**
     * 获取最小消费并发。
     *
     * @return 最小消费并发
     */
    default int getMinConcurrency() {
        return 1;
    }

    /**
     * 获取最大消费并发。
     *
     * @return 最大消费并发
     */
    default int getMaxConcurrency() {
        return 1;
    }

    /**
     * 获取消费者编码。
     *
     * @return 消费者编码
     */
    String consumerCode();

    /**
     * 消费事务消息。
     *
     * @param context 消费上下文
     * @param payload 消息负载
     */
    void consume(ConsumeContext context, T payload);
}
