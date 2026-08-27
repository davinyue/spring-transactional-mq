package org.rdlinux.transactionalmq.kafka;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MqType;
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

    /**
     * Kafka 消费者工厂
     */
    private final ConsumerFactory<String, byte[]> consumerFactory;
    /**
     * Kafka 消费调用器
     */
    private final KafkaConsumerInvoker kafkaConsumerInvoker;
    /**
     * 消息负载序列化器
     */
    private final MessagePayloadSerializer messagePayloadSerializer;
    /**
     * 消费幂等服务
     */
    private final ConsumeIdempotentService consumeIdempotentService;
    /**
     * Spring 应用上下文
     */
    private final ApplicationContext applicationContext;
    /**
     * 事务消息事务服务
     */
    private final TxnMqTransactionalService txnMqTransactionalService;
    /**
     * 消息发布服务
     */
    private final MessagePublishService messagePublishService;
    /**
     * 已创建的 Kafka 监听容器
     */
    private final List<ConcurrentMessageListenerContainer<String, byte[]>> containers = new ArrayList<>();

    /**
     * 构造 Kafka 消费者注册器
     *
     * @param consumerFactory Kafka 消费者工厂
     * @param kafkaConsumerInvoker Kafka 消费调用器
     * @param messagePayloadSerializer 消息负载序列化器
     * @param consumeIdempotentService 消费幂等服务
     * @param applicationContext Spring 应用上下文
     * @param txnMqTransactionalService 事务消息事务服务
     * @param messagePublishService 消息发布服务
     */
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

    /**
     * 初始化并注册 Kafka 消费者
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void afterSingletonsInstantiated() {
        if (this.applicationContext == null) {
            return;
        }
        Map<String, TransactionalMessageConsumer> consumers =
                this.applicationContext.getBeansOfType(TransactionalMessageConsumer.class);
        for (TransactionalMessageConsumer consumer : consumers.values()) {
            if (!MqType.KAFKA.equals(consumer.getSupportMqType())) {
                continue;
            }
            this.consume(consumer);
        }
    }

    /**
     * 注册并启动 Kafka 消费者
     *
     * @param consumer 消费者
     */
    public void consume(TransactionalMessageConsumer<?> consumer) {
        this.validateRetryPolicy(consumer);
        ConcurrentMessageListenerContainer<String, byte[]> container = this.createContainer(consumer);
        this.containers.add(container);
        this.startContainer(container);
    }

    /**
     * 创建 Kafka 消息监听容器
     *
     * @param consumer 消费者
     * @return Kafka 监听容器
     */
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
                new ConcurrentMessageListenerContainer<>(this.consumerFactory, containerProperties);
        container.setConcurrency(this.resolveConcurrency(consumer));
        container.setAutoStartup(false);
        container.getContainerProperties().setMissingTopicsFatal(false);
        return container;
    }

    /**
     * 启动 Kafka 消息监听容器
     *
     * @param container Kafka 监听容器
     */
    protected void startContainer(ConcurrentMessageListenerContainer<String, byte[]> container) {
        container.start();
    }

    /**
     * 计算消费者并发数
     *
     * @param consumer 消费者
     * @return 并发数
     */
    private int resolveConcurrency(TransactionalMessageConsumer<?> consumer) {
        int minConcurrency = Math.max(consumer.getMinConcurrency(), 1);
        int maxConcurrency = Math.max(consumer.getMaxConcurrency(), 1);
        return Math.max(minConcurrency, maxConcurrency);
    }

    /**
     * 校验消费者重试策略
     *
     * @param consumer 消费者
     */
    private void validateRetryPolicy(TransactionalMessageConsumer<?> consumer) {
        if (consumer.getConsumeRetryPolicy() == null) {
            throw new IllegalArgumentException("consume retry policy must not be null: " + consumer.consumerCode());
        }
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
