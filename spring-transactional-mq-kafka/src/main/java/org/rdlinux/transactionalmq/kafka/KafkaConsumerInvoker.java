package org.rdlinux.transactionalmq.kafka;

import org.rdlinux.transactionalmq.api.consumer.QueueMsgHandleRet;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;

/**
 * Kafka 消费调用器。
 */
public class KafkaConsumerInvoker {

    public <T> QueueMsgHandleRet invoke(TransactionalMessageConsumer<T> consumer, ConsumeContext context, T payload) {
        return consumer.consume(context, payload);
    }
}
