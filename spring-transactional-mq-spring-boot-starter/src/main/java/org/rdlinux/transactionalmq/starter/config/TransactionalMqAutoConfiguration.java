package org.rdlinux.transactionalmq.starter.config;

import org.flywaydb.core.Flyway;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.rdlinux.ezmybatis.core.dao.EzDao;
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
import java.util.Collections;
import java.util.List;

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

    /**
     * 创建派发线程唤醒协调器
     *
     * @return 派发线程唤醒协调器
     */
    @Bean
    @ConditionalOnMissingBean(MessageDispatchWakeupCoordinator.class)
    public MessageDispatchWakeupCoordinator messageDispatchWakeupCoordinator() {
        return new MessageDispatchWakeupCoordinator();
    }

    /**
     * 创建默认消息负载序列化器
     *
     * @return 消息负载序列化器
     */
    @Bean
    @ConditionalOnMissingBean(MessagePayloadSerializer.class)
    public MessagePayloadSerializer messagePayloadSerializer() {
        return new LuavaJsonMessagePayloadSerializer();
    }

    /**
     * 创建事务消息表 Flyway 迁移执行器。
     *
     * @param dataSource        数据源
     * @param mybatisProperties MyBatis 配置
     * @param properties        事务消息配置
     * @return 迁移执行器
     */
    @Bean
    @ConditionalOnProperty(prefix = TransactionalMqProperties.PREFIX, name = "auto-init-schema", havingValue = "true")
    @ConditionalOnClass(Flyway.class)
    @ConditionalOnBean({DataSource.class, EzDao.class})
    @ConditionalOnMissingBean(TransactionalMqFlywayInitializer.class)
    public TransactionalMqFlywayInitializer transactionalMqFlywayInitializer(DataSource dataSource,
                                                                              MybatisProperties mybatisProperties,
                                                                              TransactionalMqProperties properties) {
        return new TransactionalMqFlywayInitializer(dataSource, mybatisProperties, properties);
    }

    /**
     * 创建事务消息仓储
     *
     * @return 事务消息仓储
     */
    @Bean
    @ConditionalOnClass(EzDao.class)
    @ConditionalOnBean(EzDao.class)
    @ConditionalOnMissingBean(TransactionalMessageRepository.class)
    public TransactionalMessageRepository transactionalMessageRepository() {
        return new EzMybatisTransactionalMessageRepository();
    }

    /**
     * 创建已消费消息仓储
     *
     * @return 已消费消息仓储
     */
    @Bean
    @ConditionalOnClass(EzDao.class)
    @ConditionalOnBean(EzDao.class)
    @ConditionalOnMissingBean(ConsumedMessageRepository.class)
    public ConsumedMessageRepository consumedMessageRepository() {
        return new EzMybatisConsumedMessageRepository();
    }

    /**
     * 创建发送日志仓储
     *
     * @return 发送日志仓储
     */
    @Bean
    @ConditionalOnClass(EzDao.class)
    @ConditionalOnBean(EzDao.class)
    @ConditionalOnMissingBean(MessageSendLogRepository.class)
    public MessageSendLogRepository messageSendLogRepository() {
        return new EzMybatisMessageSendLogRepository();
    }

    /**
     * 创建消息发布服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param messagePayloadSerializer       消息负载序列化器
     * @param wakeupCoordinator              派发线程唤醒协调器
     * @param mqProducerRouter               MQ 生产者路由器
     * @return 消息发布服务
     */
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

    /**
     * 创建消费幂等服务
     *
     * @param consumedMessageRepository 已消费消息仓储
     * @return 消费幂等服务
     */
    @Bean
    @ConditionalOnBean(ConsumedMessageRepository.class)
    @ConditionalOnMissingBean(ConsumeIdempotentService.class)
    public ConsumeIdempotentService consumeIdempotentService(ConsumedMessageRepository consumedMessageRepository) {
        return new ConsumeIdempotentService(consumedMessageRepository);
    }

    /**
     * 创建已消费消息清理服务
     *
     * @param consumedMessageRepository 已消费消息仓储
     * @return 已消费消息清理服务
     */
    @Bean
    @ConditionalOnBean(ConsumedMessageRepository.class)
    @ConditionalOnMissingBean(ConsumedMessageCleanupService.class)
    public ConsumedMessageCleanupService consumedMessageCleanupService(
            ConsumedMessageRepository consumedMessageRepository) {
        return new ConsumedMessageCleanupService(consumedMessageRepository);
    }

    /**
     * 创建已消费消息清理任务
     *
     * @param consumedMessageCleanupService 已消费消息清理服务
     * @param properties                    事务消息配置
     * @return 已消费消息清理任务
     */
    @Bean
    @ConditionalOnBean(ConsumedMessageCleanupService.class)
    @ConditionalOnMissingBean(ConsumedMessageCleanupScheduler.class)
    public ConsumedMessageCleanupScheduler consumedMessageCleanupScheduler(
            ConsumedMessageCleanupService consumedMessageCleanupService, TransactionalMqProperties properties) {
        return new ConsumedMessageCleanupScheduler(consumedMessageCleanupService,
                properties.getConsumeRecordRetentionDays(), properties.getConsumeRecordCleanupBatchSize());
    }

    /**
     * 创建事务消息清理服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @return 事务消息清理服务
     */
    @Bean
    @ConditionalOnBean(TransactionalMessageRepository.class)
    @ConditionalOnMissingBean(TransactionalMessageCleanupService.class)
    public TransactionalMessageCleanupService transactionalMessageCleanupService(
            TransactionalMessageRepository transactionalMessageRepository) {
        return new TransactionalMessageCleanupService(transactionalMessageRepository);
    }

    /**
     * 创建事务消息清理任务
     *
     * @param transactionalMessageCleanupService 事务消息清理服务
     * @param properties                         事务消息配置
     * @return 事务消息清理任务
     */
    @Bean
    @ConditionalOnBean(TransactionalMessageCleanupService.class)
    @ConditionalOnMissingBean(TransactionalMessageCleanupScheduler.class)
    public TransactionalMessageCleanupScheduler transactionalMessageCleanupScheduler(
            TransactionalMessageCleanupService transactionalMessageCleanupService,
            TransactionalMqProperties properties) {
        return new TransactionalMessageCleanupScheduler(transactionalMessageCleanupService,
                properties.getSuccessMessageRetentionDays(), properties.getSuccessMessageCleanupBatchSize());
    }

    /**
     * 创建事务消息事务边界服务
     *
     * @return 事务消息事务边界服务
     */
    @Bean
    @ConditionalOnClass(TxnMqTransactionalService.class)
    @ConditionalOnMissingBean(TxnMqTransactionalService.class)
    public TxnMqTransactionalService txnMqTransactionalService() {
        return new TxnMqTransactionalService();
    }

    /**
     * 创建 MQ 生产者路由器
     *
     * @param mqProducerAdapters MQ 生产者适配器提供器
     * @return MQ 生产者路由器
     */
    @Bean
    @ConditionalOnMissingBean(MqProducerRouter.class)
    public MqProducerRouter mqProducerRouter(ObjectProvider<List<MqProducerAdapter>> mqProducerAdapters) {
        List<MqProducerAdapter> adapters = mqProducerAdapters.getIfAvailable();
        return new MqProducerRouter(adapters == null ? Collections.emptyList() : adapters);
    }

    /**
     * 创建事务消息启动校验器
     *
     * @param mqProducerRouter MQ 生产者路由器
     * @return 启动校验器
     */
    @Bean
    @ConditionalOnMissingBean(TransactionalMqStartupValidator.class)
    public TransactionalMqStartupValidator transactionalMqStartupValidator(MqProducerRouter mqProducerRouter) {
        return new TransactionalMqStartupValidator(mqProducerRouter);
    }

    /**
     * 创建消息派发服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param mqProducerRouter               MQ 生产者路由器
     * @param messageSendLogRepository       发送日志仓储
     * @return 消息派发服务
     */
    @Bean
    @ConditionalOnBean({TransactionalMessageRepository.class, MqProducerRouter.class, MessageSendLogRepository.class})
    @ConditionalOnMissingBean(MessageDispatchService.class)
    public MessageDispatchService messageDispatchService(
            TransactionalMessageRepository transactionalMessageRepository, MqProducerRouter mqProducerRouter,
            MessageSendLogRepository messageSendLogRepository) {
        return new MessageDispatchService(transactionalMessageRepository, mqProducerRouter, messageSendLogRepository);
    }

    /**
     * 创建事务消息后台派发任务
     *
     * @param messageDispatchService 消息派发服务
     * @param properties             事务消息配置
     * @param wakeupCoordinator      派发线程唤醒协调器
     * @return 后台派发任务
     */
    @Bean
    @ConditionalOnBean(MessageDispatchService.class)
    @ConditionalOnMissingBean(TransactionalMessageDispatchScheduler.class)
    public TransactionalMessageDispatchScheduler transactionalMessageDispatchScheduler(
            MessageDispatchService messageDispatchService, TransactionalMqProperties properties,
            MessageDispatchWakeupCoordinator wakeupCoordinator) {
        return new TransactionalMessageDispatchScheduler(messageDispatchService, properties.getDispatchBatchSize(),
                properties.getDispatchIdleSleepMillis(), wakeupCoordinator);
    }

    /**
     * 创建定时任务注册器
     *
     * @param consumedMessageCleanupScheduler      已消费消息清理任务提供器
     * @param transactionalMessageCleanupScheduler 事务消息清理任务提供器
     * @param properties                           事务消息配置
     * @return 定时任务注册器
     */
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
