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
     * 已消费消息默认保留天数。
     */
    private int consumeRecordRetentionDays = 7;

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

    public int getConsumeRecordRetentionDays() {
        return consumeRecordRetentionDays;
    }

    public void setConsumeRecordRetentionDays(int consumeRecordRetentionDays) {
        this.consumeRecordRetentionDays = consumeRecordRetentionDays;
    }
}
