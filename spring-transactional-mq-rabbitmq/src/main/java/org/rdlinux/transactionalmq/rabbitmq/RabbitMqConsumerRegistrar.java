package org.rdlinux.transactionalmq.rabbitmq;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ 消费者自动注册器。
 */
public class RabbitMqConsumerRegistrar implements SmartInitializingSingleton, DisposableBean {

    private final ConnectionFactory connectionFactory;
    private final RabbitMqConsumerInvoker rabbitMqConsumerInvoker;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final ConsumeIdempotentService consumeIdempotentService;
    private final ApplicationContext applicationContext;
    private final TxnMqTransactionalService txnMqTransactionalService;
    private final List<SimpleMessageListenerContainer> containers = new ArrayList<SimpleMessageListenerContainer>();

    public RabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
                                     RabbitMqConsumerInvoker rabbitMqConsumerInvoker, MessagePayloadSerializer messagePayloadSerializer,
                                     ConsumeIdempotentService consumeIdempotentService, ApplicationContext applicationContext,
                                     TxnMqTransactionalService txnMqTransactionalService) {
        this.connectionFactory = connectionFactory;
        this.rabbitMqConsumerInvoker = rabbitMqConsumerInvoker;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.consumeIdempotentService = consumeIdempotentService;
        this.applicationContext = applicationContext;
        this.txnMqTransactionalService = txnMqTransactionalService;
    }

    /**
     * 供测试使用的最小构造器。
     */
    RabbitMqConsumerRegistrar(ConnectionFactory connectionFactory, RabbitMqConsumerInvoker rabbitMqConsumerInvoker, TxnMqTransactionalService txnMqTransactionalService) {
        this(connectionFactory, rabbitMqConsumerInvoker, null, null, null, txnMqTransactionalService);
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
            this.consume(consumer);
        }
    }

    /**
     * 注册单个消费者。
     *
     * @param mqConsumer 消费者
     */
    public void consume(TransactionalMessageConsumer<?> mqConsumer) {
        SimpleMessageListenerContainer container = this.createContainer(mqConsumer);
        this.containers.add(container);
        this.startContainer(container);
    }

    protected SimpleMessageListenerContainer createContainer(TransactionalMessageConsumer<?> mqConsumer) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(this.connectionFactory);
        container.setQueueNames(mqConsumer.getQueueName());
        container.setPrefetchCount(2);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setConcurrency(this.buildConcurrency(mqConsumer));
        if (this.messagePayloadSerializer != null && this.consumeIdempotentService != null) {
            container.setMessageListener(new RabbitMqConsumerMessageListener(mqConsumer, this.rabbitMqConsumerInvoker,
                    this.messagePayloadSerializer, this.consumeIdempotentService, this.txnMqTransactionalService));
        }
        container.afterPropertiesSet();
        return container;
    }

    protected void startContainer(SimpleMessageListenerContainer container) {
        container.start();
    }

    private String buildConcurrency(TransactionalMessageConsumer<?> mqConsumer) {
        int minConcurrency = this.normalize(mqConsumer.getMinConcurrency());
        int maxConcurrency = this.normalize(mqConsumer.getMaxConcurrency());
        if (maxConcurrency < minConcurrency) {
            maxConcurrency = minConcurrency;
        }
        return minConcurrency + "-" + maxConcurrency;
    }

    private int normalize(int concurrency) {
        return Math.max(concurrency, 1);
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
