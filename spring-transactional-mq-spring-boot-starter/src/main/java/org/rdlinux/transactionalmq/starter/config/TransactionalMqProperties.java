package org.rdlinux.transactionalmq.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事务消息 starter 配置项。
 */
@ConfigurationProperties(prefix = "transactional.mq")
public class TransactionalMqProperties {

    /**
     * 是否启用自动装配。
     */
    private boolean enabled = true;

    /**
     * 默认派发批量大小。
     */
    private int dispatchBatchSize = 100;
    /**
     * 成功消息默认保留天数。
     */
    private int successMessageRetentionDays = 7;

    /**
     * 已消费消息默认保留天数。
     */
    private int consumeRecordRetentionDays = 7;
    /**
     * 成功消息主表单次清理批量大小。
     */
    private int successMessageCleanupBatchSize = 500;
    /**
     * 已消费消息主表单次清理批量大小。
     */
    private int consumeRecordCleanupBatchSize = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDispatchBatchSize() {
        return dispatchBatchSize;
    }

    public void setDispatchBatchSize(int dispatchBatchSize) {
        this.dispatchBatchSize = dispatchBatchSize;
    }

    /**
     * 获取成功消息默认保留天数。
     *
     * @return 成功消息默认保留天数
     */
    public int getSuccessMessageRetentionDays() {
        return successMessageRetentionDays;
    }

    /**
     * 设置成功消息默认保留天数。
     *
     * @param successMessageRetentionDays 成功消息默认保留天数
     */
    public void setSuccessMessageRetentionDays(int successMessageRetentionDays) {
        this.successMessageRetentionDays = successMessageRetentionDays;
    }

    public int getConsumeRecordRetentionDays() {
        return consumeRecordRetentionDays;
    }

    public void setConsumeRecordRetentionDays(int consumeRecordRetentionDays) {
        this.consumeRecordRetentionDays = consumeRecordRetentionDays;
    }

    /**
     * 获取已消费消息主表单次清理批量大小。
     *
     * @return 单次清理批量大小
     */
    public int getConsumeRecordCleanupBatchSize() {
        return consumeRecordCleanupBatchSize;
    }

    /**
     * 设置已消费消息主表单次清理批量大小。
     *
     * @param consumeRecordCleanupBatchSize 单次清理批量大小
     */
    public void setConsumeRecordCleanupBatchSize(int consumeRecordCleanupBatchSize) {
        this.consumeRecordCleanupBatchSize = consumeRecordCleanupBatchSize;
    }

    /**
     * 获取成功消息主表单次清理批量大小。
     *
     * @return 单次清理批量大小
     */
    public int getSuccessMessageCleanupBatchSize() {
        return successMessageCleanupBatchSize;
    }

    /**
     * 设置成功消息主表单次清理批量大小。
     *
     * @param successMessageCleanupBatchSize 单次清理批量大小
     */
    public void setSuccessMessageCleanupBatchSize(int successMessageCleanupBatchSize) {
        this.successMessageCleanupBatchSize = successMessageCleanupBatchSize;
    }
}
