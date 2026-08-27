package org.rdlinux.transactionalmq.starter.config;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.rdlinux.transactionalmq.kafka.KafkaConsumerInvoker;
import org.rdlinux.transactionalmq.kafka.KafkaConsumerRegistrar;
import org.rdlinux.transactionalmq.kafka.KafkaProducerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 条件自动装配。
 */
@Configuration
@ConditionalOnClass({KafkaTemplate.class, KafkaProducerAdapter.class})
@ConditionalOnProperty(prefix = TransactionalMqProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class TransactionalMqKafkaAutoConfiguration {

    /**
     * 创建 Kafka 生产者适配器
     *
     * @param kafkaTemplate Kafka 消息模板
     * @return Kafka 生产者适配器
     */
    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(KafkaProducerAdapter.class)
    public KafkaProducerAdapter kafkaProducerAdapter(KafkaTemplate<String, byte[]> kafkaTemplate) {
        return new KafkaProducerAdapter(kafkaTemplate);
    }

    /**
     * 创建 Kafka 消费调用器
     *
     * @return Kafka 消费调用器
     */
    @Bean
    @ConditionalOnMissingBean(KafkaConsumerInvoker.class)
    public KafkaConsumerInvoker kafkaConsumerInvoker() {
        return new KafkaConsumerInvoker();
    }

    /**
     * 创建 Kafka 消费者注册器
     *
     * @param consumerFactory Kafka 消费者工厂
     * @param kafkaConsumerInvoker Kafka 消费调用器
     * @param messagePayloadSerializer 消息负载序列化器
     * @param consumeIdempotentService 消费幂等服务
     * @param applicationContext Spring 应用上下文
     * @param txnMqTransactionalService 事务消息事务服务
     * @param messagePublishService 消息发布服务
     * @return Kafka 消费者注册器
     */
    @Bean
    @ConditionalOnClass({KafkaConsumerRegistrar.class, ConsumerFactory.class, TransactionalMessageConsumer.class})
    @ConditionalOnBean({ConsumerFactory.class, KafkaConsumerInvoker.class, MessagePayloadSerializer.class,
            ConsumeIdempotentService.class, MessagePublishService.class})
    @ConditionalOnMissingBean(KafkaConsumerRegistrar.class)
    public KafkaConsumerRegistrar kafkaConsumerRegistrar(ConsumerFactory<String, byte[]> consumerFactory,
                                                         KafkaConsumerInvoker kafkaConsumerInvoker,
                                                         MessagePayloadSerializer messagePayloadSerializer,
                                                         ConsumeIdempotentService consumeIdempotentService,
                                                         ApplicationContext applicationContext,
                                                         TxnMqTransactionalService txnMqTransactionalService,
                                                         MessagePublishService messagePublishService) {
        return new KafkaConsumerRegistrar(consumerFactory, kafkaConsumerInvoker, messagePayloadSerializer,
                consumeIdempotentService, applicationContext, txnMqTransactionalService, messagePublishService);
    }
}
