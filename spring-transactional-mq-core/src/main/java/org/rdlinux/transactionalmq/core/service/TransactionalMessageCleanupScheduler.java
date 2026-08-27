package org.rdlinux.transactionalmq.core.service;

import java.util.Calendar;
import java.util.Date;

/**
 * 成功消息主表定时清理任务
 */
public class TransactionalMessageCleanupScheduler {

    /**
     * 事务消息主表清理服务
     */
    private final TransactionalMessageCleanupService transactionalMessageCleanupService;
    /**
     * 成功消息保留天数
     */
    private final int successMessageRetentionDays;
    /**
     * 单次清理批量大小
     */
    private final int successMessageCleanupBatchSize;

    /**
     * 构造成功消息主表定时清理任务
     *
     * @param transactionalMessageCleanupService 成功消息主表清理服务
     * @param successMessageRetentionDays        成功消息保留天数
     * @param successMessageCleanupBatchSize     单次清理批量大小
     */
    public TransactionalMessageCleanupScheduler(
            TransactionalMessageCleanupService transactionalMessageCleanupService,
            int successMessageRetentionDays,
            int successMessageCleanupBatchSize) {
        this.transactionalMessageCleanupService = transactionalMessageCleanupService;
        this.successMessageRetentionDays = successMessageRetentionDays;
        this.successMessageCleanupBatchSize = successMessageCleanupBatchSize;
    }

    /**
     * 定时清理成功消息主表旧记录
     */
    public void cleanupSuccessMessages() {
        Date cleanupBefore = this.resolveCleanupBefore(this.successMessageRetentionDays);
        int limit = this.successMessageCleanupBatchSize;
        int cleaned = this.transactionalMessageCleanupService.cleanupSuccessMessages(cleanupBefore, limit);
        while (cleaned >= limit && limit > 0) {
            cleaned = this.transactionalMessageCleanupService.cleanupSuccessMessages(cleanupBefore, limit);
        }
    }

    /**
     * 计算成功消息清理截止时间
     *
     * @param retentionDays 保留天数
     * @return 清理截止时间
     */
    private Date resolveCleanupBefore(int retentionDays) {
        int safeRetentionDays = Math.max(retentionDays, 1);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -safeRetentionDays);
        return calendar.getTime();
    }
}
