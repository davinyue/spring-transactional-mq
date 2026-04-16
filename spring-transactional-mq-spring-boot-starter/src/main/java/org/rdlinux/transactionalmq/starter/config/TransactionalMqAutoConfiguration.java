package org.rdlinux.transactionalmq.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;
import org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.rdlinux.transactionalmq.core.service.*;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqConsumerInvoker;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqConsumerRegistrar;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqProducerAdapter;
import org.rdlinux.transactionalmq.store.ezmybatis.repository.EzMybatisConsumedMessageRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.repository.EzMybatisMessageSendLogRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.repository.EzMybatisTransactionalMessageRepository;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 事务消息 starter 自动装配。
 *
 * <p>在检测到 `EzDao` 时注册默认的 ez-mybatis 仓储实现，
 * 再基于这些仓储装配消息发布、派发和消费归档相关服务。
 * 在检测到 `RabbitTemplate` 时注册 RabbitMQ 生产者适配器。
 * 若容器里没有 `ObjectMapper`，则提供一个最小默认实例供消息序列化使用。</p>
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(TransactionalMqProperties.class)
@AutoConfigureAfter(RabbitAutoConfiguration.class)
@ConditionalOnProperty(prefix = "transactional.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransactionalMqAutoConfiguration {

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(MessagePayloadSerializer.class)
    public MessagePayloadSerializer messagePayloadSerializer(ObjectMapper objectMapper) {
        return new LuavaJsonMessagePayloadSerializer();
    }

    @Bean
    @ConditionalOnClass(EzDao.class)
    @ConditionalOnBean(EzDao.class)
    @ConditionalOnMissingBean(TransactionalMessageRepository.class)
    public TransactionalMessageRepository transactionalMessageRepository() {
        return new EzMybatisTransactionalMessageRepository();
    }

    @Bean
    @ConditionalOnClass(EzDao.class)
    @ConditionalOnBean(EzDao.class)
    @ConditionalOnMissingBean(ConsumedMessageRepository.class)
    public ConsumedMessageRepository consumedMessageRepository() {
        return new EzMybatisConsumedMessageRepository();
    }

    @Bean
    @ConditionalOnClass(EzDao.class)
    @ConditionalOnBean(EzDao.class)
    @ConditionalOnMissingBean(MessageSendLogRepository.class)
    public MessageSendLogRepository messageSendLogRepository() {
        return new EzMybatisMessageSendLogRepository();
    }

    @Bean
    @ConditionalOnBean({TransactionalMessageRepository.class, MessagePayloadSerializer.class})
    @ConditionalOnMissingBean(MessagePublishService.class)
    public MessagePublishService messagePublishService(TransactionalMessageRepository transactionalMessageRepository,
                                                       MessagePayloadSerializer messagePayloadSerializer) {
        return new MessagePublishService(transactionalMessageRepository, messagePayloadSerializer);
    }

    @Bean
    @ConditionalOnBean(ConsumedMessageRepository.class)
    @ConditionalOnMissingBean(ConsumeIdempotentService.class)
    public ConsumeIdempotentService consumeIdempotentService(ConsumedMessageRepository consumedMessageRepository) {
        return new ConsumeIdempotentService(consumedMessageRepository);
    }

    @Bean
    @ConditionalOnBean(ConsumedMessageRepository.class)
    @ConditionalOnMissingBean(ConsumedMessageCleanupService.class)
    public ConsumedMessageCleanupService consumedMessageCleanupService(
            ConsumedMessageRepository consumedMessageRepository) {
        return new ConsumedMessageCleanupService(consumedMessageRepository);
    }

    @Bean
    @ConditionalOnBean(ConsumedMessageCleanupService.class)
    @ConditionalOnMissingBean(ConsumedMessageCleanupScheduler.class)
    public ConsumedMessageCleanupScheduler consumedMessageCleanupScheduler(
            ConsumedMessageCleanupService consumedMessageCleanupService, TransactionalMqProperties properties) {
        return new ConsumedMessageCleanupScheduler(consumedMessageCleanupService, properties);
    }

    @Bean
    @ConditionalOnBean(TransactionalMessageRepository.class)
    @ConditionalOnMissingBean(TransactionalMessageCleanupService.class)
    public TransactionalMessageCleanupService transactionalMessageCleanupService(
            TransactionalMessageRepository transactionalMessageRepository) {
        return new TransactionalMessageCleanupService(transactionalMessageRepository);
    }

    @Bean
    @ConditionalOnBean(TransactionalMessageCleanupService.class)
    @ConditionalOnMissingBean(TransactionalMessageCleanupScheduler.class)
    public TransactionalMessageCleanupScheduler transactionalMessageCleanupScheduler(
            TransactionalMessageCleanupService transactionalMessageCleanupService,
            TransactionalMqProperties properties) {
        return new TransactionalMessageCleanupScheduler(transactionalMessageCleanupService, properties);
    }

    @Bean
    @ConditionalOnClass(RabbitTemplate.class)
    @ConditionalOnBean(RabbitTemplate.class)
    @ConditionalOnMissingBean(MqProducerAdapter.class)
    public RabbitMqProducerAdapter rabbitMqProducerAdapter(RabbitTemplate rabbitTemplate) {
        return new RabbitMqProducerAdapter(rabbitTemplate);
    }

    @Bean
    @ConditionalOnClass(RabbitMqConsumerInvoker.class)
    @ConditionalOnMissingBean(RabbitMqConsumerInvoker.class)
    public RabbitMqConsumerInvoker rabbitMqConsumerInvoker() {
        return new RabbitMqConsumerInvoker();
    }

    @Bean
    @ConditionalOnClass(TxnMqTransactionalService.class)
    @ConditionalOnMissingBean(TxnMqTransactionalService.class)
    public TxnMqTransactionalService transactionalService() {
        return new TxnMqTransactionalService();
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
                                                               TxnMqTransactionalService transactionalService) {
        return new RabbitMqConsumerRegistrar(connectionFactory, rabbitMqConsumerInvoker, messagePayloadSerializer,
                consumeIdempotentService, applicationContext, transactionalService);
    }

    @Bean
    @ConditionalOnBean({TransactionalMessageRepository.class, MqProducerAdapter.class, MessageSendLogRepository.class})
    @ConditionalOnMissingBean(MessageDispatchService.class)
    public MessageDispatchService messageDispatchService(
            TransactionalMessageRepository transactionalMessageRepository, MqProducerAdapter mqProducerAdapter,
            MessageSendLogRepository messageSendLogRepository) {
        return new MessageDispatchService(transactionalMessageRepository, mqProducerAdapter, messageSendLogRepository);
    }
}
