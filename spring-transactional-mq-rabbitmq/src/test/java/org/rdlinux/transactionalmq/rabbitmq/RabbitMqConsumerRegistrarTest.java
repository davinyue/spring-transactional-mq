package org.rdlinux.transactionalmq.rabbitmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

public class RabbitMqConsumerRegistrarTest {

    @Test
    public void consumeShouldCreateContainerWithConfiguredConcurrency() {
        CapturingRegistrar registrar = new CapturingRegistrar(mock(ConnectionFactory.class), new RabbitMqConsumerInvoker());

        registrar.consume(new DemoConsumer());

        assertEquals(1, registrar.containers.size());
        SimpleMessageListenerContainer container = registrar.containers.get(0);
        assertNotNull(container.getMessageListener());
        assertEquals(AcknowledgeMode.MANUAL, container.getAcknowledgeMode());
        assertEquals(2, readIntField(container, "prefetchCount"));
        assertEquals(2, readIntField(container, "concurrentConsumers"));
        assertEquals(5, readIntField(container, "maxConcurrentConsumers"));
        assertEquals("queue.demo", container.getQueueNames()[0]);
    }

    private static int readIntField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                return ((Number) value).intValue();
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalArgumentException("field not found: " + fieldName);
    }

    private static final class CapturingRegistrar extends RabbitMqConsumerRegistrar {

        private final List<SimpleMessageListenerContainer> containers = new ArrayList<SimpleMessageListenerContainer>();

        private CapturingRegistrar(ConnectionFactory connectionFactory, RabbitMqConsumerInvoker rabbitMqConsumerInvoker) {
            super(connectionFactory, rabbitMqConsumerInvoker, mock(MessagePayloadSerializer.class),
                mock(ConsumeIdempotentService.class), null);
        }

        @Override
        protected void startContainer(SimpleMessageListenerContainer container) {
            this.containers.add(container);
        }
    }

    private static final class DemoConsumer implements TransactionalMessageConsumer<String> {

        @Override
        public String getQueueName() {
            return "queue.demo";
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
        public void consume(ConsumeContext context, String payload) {
        }
    }
}
