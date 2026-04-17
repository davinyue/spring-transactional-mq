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
@ConditionalOnProperty(prefix = "transactionalmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransactionalMqKafkaAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(KafkaProducerAdapter.class)
    public KafkaProducerAdapter kafkaProducerAdapter(KafkaTemplate<String, byte[]> kafkaTemplate) {
        return new KafkaProducerAdapter(kafkaTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(KafkaConsumerInvoker.class)
    public KafkaConsumerInvoker kafkaConsumerInvoker() {
        return new KafkaConsumerInvoker();
    }

    @Bean
    @ConditionalOnClass({KafkaConsumerRegistrar.class, ConsumerFactory.class, TransactionalMessageConsumer.class})
    @ConditionalOnBean({ConsumerFactory.class, KafkaConsumerInvoker.class, MessagePayloadSerializer.class,
            ConsumeIdempotentService.class})
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
