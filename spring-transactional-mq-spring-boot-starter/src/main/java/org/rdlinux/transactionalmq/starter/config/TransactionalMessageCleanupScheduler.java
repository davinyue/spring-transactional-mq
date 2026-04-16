package org.rdlinux.transactionalmq.starter.config;

import org.rdlinux.transactionalmq.core.service.TransactionalMessageCleanupService;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Calendar;
import java.util.Date;

/**
 * 成功消息主表定时清理任务。
 */
public class TransactionalMessageCleanupScheduler {

    private final TransactionalMessageCleanupService transactionalMessageCleanupService;
    private final TransactionalMqProperties properties;

    /**
     * 构造成功消息主表定时清理任务。
     *
     * @param transactionalMessageCleanupService 成功消息主表清理服务
     * @param properties                         starter 配置
     */
    public TransactionalMessageCleanupScheduler(
            TransactionalMessageCleanupService transactionalMessageCleanupService,
            TransactionalMqProperties properties) {
        this.transactionalMessageCleanupService = transactionalMessageCleanupService;
        this.properties = properties;
    }

    /**
     * 定时清理成功消息主表旧记录。
     */
    @Scheduled(cron = "${transactional.mq.success-message-cleanup-cron:0 30 2 * * ?}")
    public void cleanupSuccessMessages() {
        Date cleanupBefore = this.resolveCleanupBefore(this.properties.getSuccessMessageRetentionDays());
        int limit = this.properties.getSuccessMessageCleanupBatchSize();
        int cleaned = this.transactionalMessageCleanupService.cleanupSuccessMessages(cleanupBefore, limit);
        while (cleaned >= limit && limit > 0) {
            cleaned = this.transactionalMessageCleanupService.cleanupSuccessMessages(cleanupBefore, limit);
        }
    }

    private Date resolveCleanupBefore(int retentionDays) {
        int safeRetentionDays = Math.max(retentionDays, 1);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -safeRetentionDays);
        return calendar.getTime();
    }
}
