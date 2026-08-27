package org.rdlinux.transactionalmq.api.consumer;

import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.common.enums.MqType;

/**
 * 统一事务消息消费者
 *
 * @param <T> 消息负载类型
 */
public interface TransactionalMessageConsumer<T> {

    /**
     * 获取消费队列名
     *
     * @return 队列名
     */
    String getQueueName();

    /**
     * 获取消费者所属 MQ 类型
     *
     * @return MQ 类型
     */
    MqType getSupportMqType();

    /**
     * 获取最小消费并发
     *
     * @return 最小消费并发
     */
    default int getMinConcurrency() {
        return 1;
    }

    /**
     * 获取最大消费并发
     *
     * @return 最大消费并发
     */
    default int getMaxConcurrency() {
        return 1;
    }

    /**
     * 获取消费失败重试策略
     *
     * @return 消费失败重试策略
     */
    default ConsumeRetryPolicy getConsumeRetryPolicy() {
        return ConsumeRetryPolicy.nativeNack();
    }

    /**
     * 获取消费者编码
     *
     * @return 消费者编码
     */
    String consumerCode();

    /**
     * 事务开启前执行消费逻辑。
     *
     * <p>该方法在消费上下文和消息负载解析完成后、消费幂等记录及业务事务开启前执行。
     *
     * @param context       消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消息负载
     */
    default void beforeTransaction(ConsumeContext context, ConsumeHandleContext handleContext, T payload) {
    }

    /**
     * 消费事务消息事务已经开启
     *
     * @param context       消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消息负载
     */
    void consume(ConsumeContext context, ConsumeHandleContext handleContext, T payload);
}
