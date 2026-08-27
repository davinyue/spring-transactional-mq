package org.rdlinux.transactionalmq.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事务消息 starter 配置项
 */
@Setter
@Getter
@ConfigurationProperties(prefix = TransactionalMqProperties.PREFIX)
public class TransactionalMqProperties {
    /**
     * 事务消息配置前缀
     */
    public static final String PREFIX = "transactionalmq";

    /**
     * 是否启用自动装配
     */
    private boolean enabled = true;
    /**
     * 是否自动执行事务消息表的 Flyway 迁移
     */
    private boolean autoInitSchema = false;
    /**
     * 存量数据库的 Flyway 基线版本，未配置时不执行基线操作
     */
    private String schemaBaselineVersion;
    /**
     * 事务消息 Flyway schema history 表名称
     */
    private String schemaHistoryTable = "txn_mq_flyway_schema_history";
    /**
     * 默认派发批量大小
     */
    private int dispatchBatchSize = 100;
    /**
     * 派发线程空闲睡眠毫秒数
     */
    private long dispatchIdleSleepMillis = 30000L;
    /**
     * 成功消息默认保留天数数
     */
    private int successMessageRetentionDays = 7;

    /**
     * 已消费消息默认保留天数
     */
    private int consumeRecordRetentionDays = 7;
    /**
     * 成功消息主表单次清理批量大小
     */
    private int successMessageCleanupBatchSize = 500;
    /**
     * 成功消息主表清理 cron
     */
    private String successMessageCleanupCron = "0 30 2 * * ?";
    /**
     * 已消费消息主表单次清理批量大小
     */
    private int consumeRecordCleanupBatchSize = 500;
    /**
     * 已消费消息主表清理 cron
     */
    private String consumeRecordCleanupCron = "0 0 3 * * ?";
}
