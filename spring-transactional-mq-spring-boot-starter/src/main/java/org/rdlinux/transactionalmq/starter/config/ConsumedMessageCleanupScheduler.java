package org.rdlinux.transactionalmq.starter.config;

import org.rdlinux.transactionalmq.core.service.ConsumedMessageCleanupService;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Calendar;
import java.util.Date;

/**
 * 已消费消息主表定时清理任务。
 */
public class ConsumedMessageCleanupScheduler {

    private final ConsumedMessageCleanupService consumedMessageCleanupService;
    private final TransactionalMqProperties properties;

    /**
     * 构造定时清理任务。
     *
     * @param consumedMessageCleanupService 已消费消息主表清理服务
     * @param properties                    starter 配置
     */
    public ConsumedMessageCleanupScheduler(ConsumedMessageCleanupService consumedMessageCleanupService,
                                           TransactionalMqProperties properties) {
        this.consumedMessageCleanupService = consumedMessageCleanupService;
        this.properties = properties;
    }

    /**
     * 定时清理消费主表旧记录。
     */
    @Scheduled(cron = "${transactional.mq.consume-record-cleanup-cron:0 0 3 * * ?}")
    public void cleanupConsumedMessages() {
        Date cleanupBefore = this.resolveCleanupBefore(this.properties.getConsumeRecordRetentionDays());
        int limit = this.properties.getConsumeRecordCleanupBatchSize();
        int cleaned = this.consumedMessageCleanupService.cleanupOlderThan(cleanupBefore, limit);
        while (cleaned >= limit && limit > 0) {
            cleaned = this.consumedMessageCleanupService.cleanupOlderThan(cleanupBefore, limit);
        }
    }

    private Date resolveCleanupBefore(int retentionDays) {
        int safeRetentionDays = Math.max(retentionDays, 1);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -safeRetentionDays);
        return calendar.getTime();
    }
}
