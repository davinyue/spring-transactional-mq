package org.rdlinux.transactionalmq.kafka;

import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.QueueMsgHandleRet;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class KafkaConsumerRegistrarTest {

    @Test
    public void consumeShouldCreateContainerWithConfiguredConcurrency() {
        CapturingRegistrar registrar = new CapturingRegistrar(mock(ConsumerFactory.class), new KafkaConsumerInvoker());

        registrar.consume(new DemoConsumer());

        assertEquals(1, registrar.containers.size());
        ConcurrentMessageListenerContainer<String, byte[]> container = registrar.containers.get(0);
        assertNotNull(container.getContainerProperties().getMessageListener());
        assertEquals(ContainerProperties.AckMode.MANUAL, container.getContainerProperties().getAckMode());
        assertEquals(5, container.getConcurrency());
        assertEquals("topic.demo", container.getContainerProperties().getTopics()[0]);
    }

    private static final class CapturingRegistrar extends KafkaConsumerRegistrar {

        private final List<ConcurrentMessageListenerContainer<String, byte[]>> containers =
                new ArrayList<ConcurrentMessageListenerContainer<String, byte[]>>();

        private CapturingRegistrar(ConsumerFactory<String, byte[]> consumerFactory,
                                   KafkaConsumerInvoker kafkaConsumerInvoker) {
            super(consumerFactory, kafkaConsumerInvoker, mock(MessagePayloadSerializer.class),
                    mock(ConsumeIdempotentService.class), null, new TxnMqTransactionalService(), null);
        }

        @Override
        protected void startContainer(ConcurrentMessageListenerContainer<String, byte[]> container) {
            this.containers.add(container);
        }
    }

    private static final class DemoConsumer implements TransactionalMessageConsumer<String> {

        @Override
        public String getQueueName() {
            return "topic.demo";
        }

        @Override
        public int getMinConcurrency() {
            return 2;
        }

        @Override
        public int getMaxConcurrency() {
            return 5;
        }

        @Override
        public String consumerCode() {
            return "consumer-demo";
        }

        @Override
        public QueueMsgHandleRet consume(ConsumeContext context, String payload) {
            return QueueMsgHandleRet.DEFAULT();
        }
    }
}
