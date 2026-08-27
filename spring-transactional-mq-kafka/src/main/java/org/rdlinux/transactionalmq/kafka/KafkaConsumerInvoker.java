package org.rdlinux.transactionalmq.kafka;

import org.rdlinux.transactionalmq.api.consumer.ConsumeHandleContext;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;

/**
 * Kafka 消费调用器。
 */
public class KafkaConsumerInvoker {

    /**
     * 调用 Kafka 消费者处理消息
     *
     * @param consumer 消费者
     * @param context 消费上下文
     * @param handleContext 消费处理上下文
     * @param payload       消息负载
     * @param <T>           负载类型
     */
    public <T> void invoke(TransactionalMessageConsumer<T> consumer, ConsumeContext context,
                           ConsumeHandleContext handleContext, T payload) {
        consumer.consume(context, handleContext, payload);
    }

    /**
     * 调用 Kafka 消费者事务开启前处理逻辑
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
