package org.rdlinux.transactionalmq.rabbitmq;

import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.ConsumeHandleContext;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.common.enums.MqType;

import static org.junit.Assert.assertEquals;

public class RabbitMqConsumerInvokerTest {

    @Test
    public void invokeShouldPassContextAndPayloadToConsumer() {
        RabbitMqConsumerInvoker invoker = new RabbitMqConsumerInvoker();
        ConsumeContext context = new ConsumeContext().setId("msg-3").setMessageKey("key-3")
                .setConsumerCode("consumer-3");
        RecordingConsumer consumer = new RecordingConsumer();

        ConsumeHandleContext handleContext = ConsumeHandleContext.DEFAULT();
        invoker.invoke(consumer, context, handleContext, "payload-3");

        assertEquals(context, consumer.context);
        assertEquals("payload-3", consumer.payload);
    }

    private static final class RecordingConsumer implements TransactionalMessageConsumer<String> {

        private ConsumeContext context;
        private String payload;

        @Override
        public String getQueueName() {
            return "queue.consumer.3";
        }

        @Override
        public MqType getSupportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public String consumerCode() {
            return "consumer-3";
        }

        @Override
        public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
            this.context = context;
            this.payload = payload;
        }
    }
}
