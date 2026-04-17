package org.rdlinux.transactionalmq.kafka;

import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.QueueMsgHandleRet;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;

import static org.junit.Assert.assertEquals;

public class KafkaConsumerInvokerTest {

    @Test
    public void invokeShouldPassContextAndPayloadToConsumer() {
        KafkaConsumerInvoker invoker = new KafkaConsumerInvoker();
        ConsumeContext context = new ConsumeContext().setId("msg-3").setMessageKey("key-3")
                .setConsumerCode("consumer-3");
        RecordingConsumer consumer = new RecordingConsumer();

        invoker.invoke(consumer, context, "payload-3");

        assertEquals(context, consumer.context);
        assertEquals("payload-3", consumer.payload);
    }

    private static final class RecordingConsumer implements TransactionalMessageConsumer<String> {

        private ConsumeContext context;
        private String payload;

        @Override
        public String getQueueName() {
            return "topic.consumer.3";
        }

        @Override
        public String consumerCode() {
            return "consumer-3";
        }

        @Override
        public QueueMsgHandleRet consume(ConsumeContext context, String payload) {
            this.context = context;
            this.payload = payload;
            return QueueMsgHandleRet.DEFAULT();
        }
    }
}
