package org.rdlinux.transactionalmq.kafka;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kafka 消费者自动注册器。
 */
public class KafkaConsumerRegistrar implements SmartInitializingSingleton, DisposableBean {

    private final ConsumerFactory<String, byte[]> consumerFactory;
    private final KafkaConsumerInvoker kafkaConsumerInvoker;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final ConsumeIdempotentService consumeIdempotentService;
    private final ApplicationContext applicationContext;
    private final TxnMqTransactionalService txnMqTransactionalService;
    private final MessagePublishService messagePublishService;
    private final List<ConcurrentMessageListenerContainer<String, byte[]>> containers =
            new ArrayList<ConcurrentMessageListenerContainer<String, byte[]>>();

    public KafkaConsumerRegistrar(ConsumerFactory<String, byte[]> consumerFactory,
                                  KafkaConsumerInvoker kafkaConsumerInvoker,
                                  MessagePayloadSerializer messagePayloadSerializer,
                                  ConsumeIdempotentService consumeIdempotentService,
                                  ApplicationContext applicationContext,
                                  TxnMqTransactionalService txnMqTransactionalService,
                                  MessagePublishService messagePublishService) {
        this.consumerFactory = consumerFactory;
        this.kafkaConsumerInvoker = kafkaConsumerInvoker;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.consumeIdempotentService = consumeIdempotentService;
        this.applicationContext = applicationContext;
        this.txnMqTransactionalService = txnMqTransactionalService;
        this.messagePublishService = messagePublishService;
    }

    KafkaConsumerRegistrar(ConsumerFactory<String, byte[]> consumerFactory,
                           KafkaConsumerInvoker kafkaConsumerInvoker,
                           TxnMqTransactionalService txnMqTransactionalService) {
        this(consumerFactory, kafkaConsumerInvoker, null, null, null, txnMqTransactionalService, null);
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

    public void consume(TransactionalMessageConsumer<?> consumer) {
        ConcurrentMessageListenerContainer<String, byte[]> container = this.createContainer(consumer);
        this.containers.add(container);
        this.startContainer(container);
    }

    protected ConcurrentMessageListenerContainer<String, byte[]> createContainer(
            TransactionalMessageConsumer<?> consumer) {
        ContainerProperties containerProperties = new ContainerProperties(consumer.getQueueName());
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL);
        if (this.messagePayloadSerializer != null && this.consumeIdempotentService != null) {
            containerProperties.setMessageListener(new KafkaConsumerMessageListener(consumer, this.kafkaConsumerInvoker,
                    this.messagePayloadSerializer, this.consumeIdempotentService, this.txnMqTransactionalService,
                    this.messagePublishService));
        }
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<String, byte[]>(this.consumerFactory, containerProperties);
        container.setConcurrency(this.resolveConcurrency(consumer));
        container.setAutoStartup(false);
        container.getContainerProperties().setMissingTopicsFatal(false);
        return container;
    }

    protected void startContainer(ConcurrentMessageListenerContainer<String, byte[]> container) {
        container.start();
    }

    private int resolveConcurrency(TransactionalMessageConsumer<?> consumer) {
        int minConcurrency = Math.max(consumer.getMinConcurrency(), 1);
        int maxConcurrency = Math.max(consumer.getMaxConcurrency(), 1);
        return Math.max(minConcurrency, maxConcurrency);
    }

    @Override
    public void destroy() {
        for (ConcurrentMessageListenerContainer<String, byte[]> container : this.containers) {
            if (container.isRunning()) {
                container.stop();
            }
        }
        this.containers.clear();
    }
}
