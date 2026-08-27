package org.rdlinux.transactionalmq.starter.config;

import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
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

    /**
     * 创建 RabbitMQ 生产者适配器
     *
     * @param rabbitTemplate RabbitMQ 模板
     * @return RabbitMQ 生产者适配器
     */
    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    @ConditionalOnMissingBean(RabbitMqProducerAdapter.class)
    public RabbitMqProducerAdapter rabbitMqProducerAdapter(RabbitTemplate rabbitTemplate) {
        return new RabbitMqProducerAdapter(rabbitTemplate);
    }

    /**
     * 创建 RabbitMQ 消费调用器
     *
     * @return RabbitMQ 消费调用器
     */
    @Bean
    @ConditionalOnMissingBean(RabbitMqConsumerInvoker.class)
    public RabbitMqConsumerInvoker rabbitMqConsumerInvoker() {
        return new RabbitMqConsumerInvoker();
    }

    /**
     * 创建 RabbitMQ 消费者注册器
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param rabbitMqConsumerInvoker RabbitMQ 消费调用器
     * @param messagePayloadSerializer 消息负载序列化器
     * @param consumeIdempotentService 消费幂等服务
     * @param applicationContext Spring 应用上下文
     * @param txnMqTransactionalService 事务消息事务服务
     * @param messagePublishService 消息发布服务
     * @return RabbitMQ 消费者注册器
     */
    @Bean
    @ConditionalOnClass({RabbitMqConsumerRegistrar.class, ConnectionFactory.class, TransactionalMessageConsumer.class})
    @ConditionalOnBean({ConnectionFactory.class, RabbitMqConsumerInvoker.class, MessagePayloadSerializer.class,
            ConsumeIdempotentService.class, MessagePublishService.class})
    @ConditionalOnMissingBean(RabbitMqConsumerRegistrar.class)
    public RabbitMqConsumerRegistrar rabbitMqConsumerRegistrar(ConnectionFactory connectionFactory,
                                                               RabbitMqConsumerInvoker rabbitMqConsumerInvoker,
                                                               MessagePayloadSerializer messagePayloadSerializer,
                                                               ConsumeIdempotentService consumeIdempotentService,
                                                               ApplicationContext applicationContext,
                                                               TxnMqTransactionalService txnMqTransactionalService,
                                                               MessagePublishService messagePublishService) {
        return new RabbitMqConsumerRegistrar(connectionFactory, rabbitMqConsumerInvoker, messagePayloadSerializer,
                consumeIdempotentService, applicationContext, txnMqTransactionalService, messagePublishService);
    }
}
