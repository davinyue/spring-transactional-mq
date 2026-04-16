package org.rdlinux.transactionalmq.rabbitmq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

/**
 * RabbitMQ 消费者自动注册器。
 */
public class RabbitMqConsumerRegistrar implements SmartInitializingSingleton, DisposableBean {

    private final ConnectionFactory connectionFactory;
    private final RabbitMqConsumerInvoker rabbitMqConsumerInvoker;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final ConsumeIdempotentService consumeIdempotentService;
    private final ApplicationContext applicationContext;
    private final List<SimpleMessageListenerContainer> containers = new ArrayList<SimpleMessageListenerContainer>();

    public RabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
            RabbitMqConsumerInvoker rabbitMqConsumerInvoker, MessagePayloadSerializer messagePayloadSerializer,
            ConsumeIdempotentService consumeIdempotentService, ApplicationContext applicationContext) {
        this.connectionFactory = connectionFactory;
        this.rabbitMqConsumerInvoker = rabbitMqConsumerInvoker;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.consumeIdempotentService = consumeIdempotentService;
        this.applicationContext = applicationContext;
    }

    /**
     * 供测试使用的最小构造器。
     */
    RabbitMqConsumerRegistrar(ConnectionFactory connectionFactory, RabbitMqConsumerInvoker rabbitMqConsumerInvoker) {
        this(connectionFactory, rabbitMqConsumerInvoker, null, null, null);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void afterSingletonsInstantiated() {
        if (this.applicationContext == null) {
            return;
        }
        Map<String, TransactionalMessageConsumer> consumers =
            this.applicationContext.getBeansOfType(TransactionalMessageConsumer.class);
        for (TransactionalMessageConsumer consumer : consumers.values()) {
            consume(consumer);
        }
    }

    /**
     * 注册单个消费者。
     *
     * @param mqConsumer 消费者
     */
    public void consume(TransactionalMessageConsumer<?> mqConsumer) {
        SimpleMessageListenerContainer container = createContainer(mqConsumer);
        this.containers.add(container);
        startContainer(container);
    }

    protected SimpleMessageListenerContainer createContainer(TransactionalMessageConsumer<?> mqConsumer) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(this.connectionFactory);
        container.setQueueNames(mqConsumer.getQueueName());
        container.setPrefetchCount(2);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setConcurrency(buildConcurrency(mqConsumer));
        if (this.messagePayloadSerializer != null && this.consumeIdempotentService != null) {
            container.setMessageListener(new RabbitMqConsumerMessageListener(mqConsumer, this.rabbitMqConsumerInvoker,
                this.messagePayloadSerializer, this.consumeIdempotentService));
        }
        container.afterPropertiesSet();
        return container;
    }

    protected void startContainer(SimpleMessageListenerContainer container) {
        container.start();
    }

    private String buildConcurrency(TransactionalMessageConsumer<?> mqConsumer) {
        int minConcurrency = normalize(mqConsumer.getMinConcurrency());
        int maxConcurrency = normalize(mqConsumer.getMaxConcurrency());
        if (maxConcurrency < minConcurrency) {
            maxConcurrency = minConcurrency;
        }
        return minConcurrency + "-" + maxConcurrency;
    }

    private int normalize(int concurrency) {
        return concurrency < 1 ? 1 : concurrency;
    }

    @Override
    public void destroy() {
        for (SimpleMessageListenerContainer container : this.containers) {
            if (container.isRunning()) {
                container.stop();
            }
            container.destroy();
        }
        this.containers.clear();
    }
}
