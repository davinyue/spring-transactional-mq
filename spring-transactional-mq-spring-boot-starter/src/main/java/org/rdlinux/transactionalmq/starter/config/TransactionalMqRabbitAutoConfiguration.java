package org.rdlinux.transactionalmq.starter.config;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.TxnMqTransactionalService;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqConsumerInvoker;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqConsumerRegistrar;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqProducerAdapter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 条件自动装配。
 */
@Configuration
@AutoConfigureAfter(RabbitAutoConfiguration.class)
@ConditionalOnClass({RabbitTemplate.class, RabbitMqProducerAdapter.class})
@ConditionalOnProperty(prefix = TransactionalMqProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class TransactionalMqRabbitAutoConfiguration {

    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    @ConditionalOnMissingBean(RabbitMqProducerAdapter.class)
    public RabbitMqProducerAdapter rabbitMqProducerAdapter(RabbitTemplate rabbitTemplate) {
        return new RabbitMqProducerAdapter(rabbitTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RabbitMqConsumerInvoker.class)
    public RabbitMqConsumerInvoker rabbitMqConsumerInvoker() {
        return new RabbitMqConsumerInvoker();
    }

    @Bean
    @ConditionalOnClass({RabbitMqConsumerRegistrar.class, ConnectionFactory.class, TransactionalMessageConsumer.class})
    @ConditionalOnBean({ConnectionFactory.class, RabbitMqConsumerInvoker.class, MessagePayloadSerializer.class,
            ConsumeIdempotentService.class})
    @ConditionalOnMissingBean(RabbitMqConsumerRegistrar.class)
    public RabbitMqConsumerRegistrar rabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
                                                               RabbitMqConsumerInvoker rabbitMqConsumerInvoker,
                                                               MessagePayloadSerializer messagePayloadSerializer,
                                                               ConsumeIdempotentService consumeIdempotentService,
                                                               ApplicationContext applicationContext,
                                                               TxnMqTransactionalService txnMqTransactionalService) {
        return new RabbitMqConsumerRegistrar(connectionFactory, rabbitMqConsumerInvoker, messagePayloadSerializer,
                consumeIdempotentService, applicationContext, txnMqTransactionalService);
    }
}
