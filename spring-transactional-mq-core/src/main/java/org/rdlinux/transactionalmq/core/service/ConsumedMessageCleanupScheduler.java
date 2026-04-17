package org.rdlinux.transactionalmq.core.service;

import java.util.Calendar;
import java.util.Date;

/**
 * 已消费消息主表定时清理任务
 */
public class ConsumedMessageCleanupScheduler {
    /**
     * 已消费消息主表清理服务
     */
    private final ConsumedMessageCleanupService consumedMessageCleanupService;
    /**
     * 已消费消息保留天数
     */
    private final int consumeRecordRetentionDays;
    /**
     * 单次清理批量大小
     */
    private final int consumeRecordCleanupBatchSize;

    /**
     * 构造定时清理任务
     *
     * @param consumedMessageCleanupService 已消费消息主表清理服务
     * @param consumeRecordRetentionDays    已消费消息保留天数
     * @param consumeRecordCleanupBatchSize 单次清理批量大小
     */
    public ConsumedMessageCleanupScheduler(ConsumedMessageCleanupService consumedMessageCleanupService,
                                           int consumeRecordRetentionDays, int consumeRecordCleanupBatchSize) {
        this.consumedMessageCleanupService = consumedMessageCleanupService;
        this.consumeRecordRetentionDays = consumeRecordRetentionDays;
        this.consumeRecordCleanupBatchSize = consumeRecordCleanupBatchSize;
    }

    /**
     * 定时清理消费主表旧记录
     */
    public void cleanupConsumedMessages() {
        Date cleanupBefore = this.resolveCleanupBefore(this.consumeRecordRetentionDays);
        int limit = this.consumeRecordCleanupBatchSize;
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
