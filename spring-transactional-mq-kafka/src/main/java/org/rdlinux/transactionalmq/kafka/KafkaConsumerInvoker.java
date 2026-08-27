package org.rdlinux.transactionalmq.kafka;

import org.rdlinux.transactionalmq.api.consumer.QueueMsgHandleRet;
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
     * @param payload 消息负载
     * @param <T> 负载类型
     * @return 消息处理结果
     */
    public <T> QueueMsgHandleRet invoke(TransactionalMessageConsumer<T> consumer, ConsumeContext context, T payload) {
        return consumer.consume(context, payload);
    }
}
