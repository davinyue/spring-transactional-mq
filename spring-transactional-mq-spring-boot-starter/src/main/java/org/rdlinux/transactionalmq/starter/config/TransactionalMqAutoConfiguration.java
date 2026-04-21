package org.rdlinux.transactionalmq.starter.config;

import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.mapper.EzMapper;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;
import org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.rdlinux.transactionalmq.core.serialize.LuavaJsonMessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.*;
import org.rdlinux.transactionalmq.core.service.impl.MessageDispatchWakeupCoordinator;
import org.rdlinux.transactionalmq.store.ezmybatis.repository.EzMybatisConsumedMessageRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.repository.EzMybatisMessageSendLogRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.repository.EzMybatisTransactionalMessageRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * 事务消息 starter 自动装配
 *
 * <p>在检测到 `EzDao` 时注册默认的 ez-mybatis 仓储实现，
 * 再基于这些仓储装配消息发布、派发和消费归档相关服务
 * 在检测到 `RabbitTemplate` 时注册 RabbitMQ 生产者适配器</p>
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(TransactionalMqProperties.class)
@ConditionalOnProperty(prefix = TransactionalMqProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class TransactionalMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageDispatchWakeupCoordinator.class)
    public MessageDispatchWakeupCoordinator messageDispatchWakeupCoordinator() {
        return new MessageDispatchWakeupCoordinator();
    }

    @Bean
    @ConditionalOnMissingBean(MessagePayloadSerializer.class)
    public MessagePayloadSerializer messagePayloadSerializer() {
        return new LuavaJsonMessagePayloadSerializer();
    }

    @Bean
    @ConditionalOnProperty(prefix = TransactionalMqProperties.PREFIX, name = "auto-init-schema",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(TransactionalMqSchemaInitializer.class)
    public TransactionalMqSchemaInitializer transactionalMqSchemaInitializer(DataSource dataSource,
                                                                             MybatisProperties mybatisProperties,
                                                                             EzMapper ezMapper) {
        return new TransactionalMqSchemaInitializer(dataSource, mybatisProperties.getConfiguration(), ezMapper);
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
                                                       MessagePayloadSerializer messagePayloadSerializer,
                                                       MessageDispatchWakeupCoordinator wakeupCoordinator,
                                                       MqProducerRouter mqProducerRouter) {
        return new MessagePublishService(transactionalMessageRepository, messagePayloadSerializer,
                wakeupCoordinator, mqProducerRouter);
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
        return new ConsumedMessageCleanupScheduler(consumedMessageCleanupService,
                properties.getConsumeRecordRetentionDays(), properties.getConsumeRecordCleanupBatchSize());
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
        return new TransactionalMessageCleanupScheduler(transactionalMessageCleanupService,
                properties.getSuccessMessageRetentionDays(), properties.getSuccessMessageCleanupBatchSize());
    }

    @Bean
    @ConditionalOnClass(TxnMqTransactionalService.class)
    @ConditionalOnMissingBean(TxnMqTransactionalService.class)
    public TxnMqTransactionalService txnMqTransactionalService() {
        return new TxnMqTransactionalService();
    }

    @Bean
    @ConditionalOnMissingBean(MqProducerRouter.class)
    public MqProducerRouter mqProducerRouter(ObjectProvider<java.util.List<MqProducerAdapter>> mqProducerAdapters) {
        java.util.List<MqProducerAdapter> adapters = mqProducerAdapters.getIfAvailable();
        return new MqProducerRouter(adapters == null ? java.util.Collections.<MqProducerAdapter>emptyList() : adapters);
    }

    @Bean
    @ConditionalOnMissingBean(TransactionalMqStartupValidator.class)
    public TransactionalMqStartupValidator transactionalMqStartupValidator(MqProducerRouter mqProducerRouter) {
        return new TransactionalMqStartupValidator(mqProducerRouter);
    }

    @Bean
    @ConditionalOnBean({TransactionalMessageRepository.class, MqProducerRouter.class, MessageSendLogRepository.class})
    @ConditionalOnMissingBean(MessageDispatchService.class)
    public MessageDispatchService messageDispatchService(
            TransactionalMessageRepository transactionalMessageRepository, MqProducerRouter mqProducerRouter,
            MessageSendLogRepository messageSendLogRepository) {
        return new MessageDispatchService(transactionalMessageRepository, mqProducerRouter, messageSendLogRepository);
    }

    @Bean
    @ConditionalOnBean(MessageDispatchService.class)
    @ConditionalOnMissingBean(TransactionalMessageDispatchScheduler.class)
    public TransactionalMessageDispatchScheduler transactionalMessageDispatchScheduler(
            MessageDispatchService messageDispatchService, TransactionalMqProperties properties,
            MessageDispatchWakeupCoordinator wakeupCoordinator) {
        return new TransactionalMessageDispatchScheduler(messageDispatchService, properties.getDispatchBatchSize(),
                properties.getDispatchIdleSleepMillis(), wakeupCoordinator);
    }

    @Bean
    @ConditionalOnMissingBean(TransactionalMqScheduledTaskConfigurer.class)
    public TransactionalMqScheduledTaskConfigurer transactionalMqScheduledTaskConfigurer(
            ObjectProvider<ConsumedMessageCleanupScheduler> consumedMessageCleanupScheduler,
            ObjectProvider<TransactionalMessageCleanupScheduler> transactionalMessageCleanupScheduler,
            TransactionalMqProperties properties) {
        return new TransactionalMqScheduledTaskConfigurer(consumedMessageCleanupScheduler.getIfAvailable(),
                transactionalMessageCleanupScheduler.getIfAvailable(), properties);
    }
}
