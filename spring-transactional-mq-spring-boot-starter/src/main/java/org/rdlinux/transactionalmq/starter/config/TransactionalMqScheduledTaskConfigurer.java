package org.rdlinux.transactionalmq.starter.config;

import org.rdlinux.transactionalmq.core.service.ConsumedMessageCleanupScheduler;
import org.rdlinux.transactionalmq.core.service.TransactionalMessageCleanupScheduler;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 事务消息定时任务编码注册器
 */
public class TransactionalMqScheduledTaskConfigurer implements SchedulingConfigurer {

    private final ConsumedMessageCleanupScheduler consumedMessageCleanupScheduler;
    private final TransactionalMessageCleanupScheduler transactionalMessageCleanupScheduler;
    private final TransactionalMqProperties properties;

    /**
     * 构造定时任务注册器
     *
     * @param consumedMessageCleanupScheduler      消费记录清理执行器
     * @param transactionalMessageCleanupScheduler 成功消息清理执行器
     * @param properties                           starter 配置
     */
    public TransactionalMqScheduledTaskConfigurer(
            ConsumedMessageCleanupScheduler consumedMessageCleanupScheduler,
            TransactionalMessageCleanupScheduler transactionalMessageCleanupScheduler,
            TransactionalMqProperties properties) {
        this.consumedMessageCleanupScheduler = consumedMessageCleanupScheduler;
        this.transactionalMessageCleanupScheduler = transactionalMessageCleanupScheduler;
        this.properties = properties;
    }

    @Override
    public void configureTasks(@NonNull ScheduledTaskRegistrar taskRegistrar) {
        if (this.consumedMessageCleanupScheduler != null) {
            taskRegistrar.addCronTask(this.consumedMessageCleanupScheduler::cleanupConsumedMessages,
                    this.properties.getConsumeRecordCleanupCron());
        }
        if (this.transactionalMessageCleanupScheduler != null) {
            taskRegistrar.addCronTask(this.transactionalMessageCleanupScheduler::cleanupSuccessMessages,
                    this.properties.getSuccessMessageCleanupCron());
        }
    }
}
