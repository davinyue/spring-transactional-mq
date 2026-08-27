package org.rdlinux.transactionalmq.rabbitmq;

import org.rdlinux.transactionalmq.api.consumer.ConsumeHandleContext;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;

/**
 * RabbitMQ 消费调用器
 */
public class RabbitMqConsumerInvoker {

    /**
     * 将解析后的上下文和负载交给统一消费者
     *
     * @param consumer 统一消费者
     * @param context  消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消息负载
     * @param <T>           负载类型
     */
    public <T> void invoke(TransactionalMessageConsumer<T> consumer, ConsumeContext context,
                           ConsumeHandleContext handleContext, T payload) {
        consumer.consume(context, handleContext, payload);
    }

    /**
     * 调用 RabbitMQ 消费者事务开启前处理逻辑
     *
     * @param consumer 消费者
     * @param context       消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消息负载
     * @param <T>           负载类型
     */
    public <T> void invokeBeforeTransaction(TransactionalMessageConsumer<T> consumer, ConsumeContext context,
                                            ConsumeHandleContext handleContext, T payload) {
        consumer.beforeTransaction(context, handleContext, payload);
    }
}
