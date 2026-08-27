package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

public class SqlEnumColumnTypeTest {

    @Test
    public void mysqlSqlShouldUseIntColumnsForEnums() throws Exception {
        String sql = readSql("src/main/resources/transactionalmq/db/migration/mysql/V1__init.sql");

        assertContains(sql, "mq_type INT NOT NULL");
        assertContains(sql, "message_status INT NOT NULL");
        assertContains(sql, "consume_status INT NOT NULL");
        assertContains(sql, "send_status INT NOT NULL");
        Assert.assertFalse(sql.contains("mq_type VARCHAR"));
        Assert.assertFalse(sql.contains("message_status VARCHAR"));
        Assert.assertFalse(sql.contains("consume_status VARCHAR"));
        Assert.assertFalse(sql.contains("send_status VARCHAR"));
    }

    @Test
    public void otherDatabaseSqlShouldUseIntegerColumnsForEnums() throws Exception {
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/postgresql/V1__init.sql"), "mq_type INTEGER NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/postgresql/V1__init.sql"), "message_status INTEGER NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/postgresql/V1__init.sql"), "consume_status INTEGER NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/postgresql/V1__init.sql"), "send_status INTEGER NOT NULL");

        assertContains(readSql("src/main/resources/transactionalmq/db/migration/oracle/V1__init.sql"), "mq_type NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/oracle/V1__init.sql"), "message_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/oracle/V1__init.sql"), "consume_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/oracle/V1__init.sql"), "send_status NUMBER(10) NOT NULL");

        assertContains(readSql("src/main/resources/transactionalmq/db/migration/dm/V1__init.sql"), "mq_type NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/dm/V1__init.sql"), "message_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/dm/V1__init.sql"), "consume_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/dm/V1__init.sql"), "send_status NUMBER(10) NOT NULL");

        assertContains(readSql("src/main/resources/transactionalmq/db/migration/sqlserver/V1__init.sql"), "mq_type INT NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/sqlserver/V1__init.sql"), "message_status INT NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/sqlserver/V1__init.sql"), "consume_status INT NOT NULL");
        assertContains(readSql("src/main/resources/transactionalmq/db/migration/sqlserver/V1__init.sql"), "send_status INT NOT NULL");
    }

    /**
     * 验证五种数据库的 V1 初始化与 V2 消费重试迁移脚本。
     *
     * @throws Exception SQL 文件读取失败
     */
    @Test
    public void allDatabaseMigrationsShouldContainConsumeRetrySchema() throws Exception {
        String[] databaseDirectories = {"mysql", "oracle", "dm", "postgresql", "sqlserver"};
        for (String databaseDirectory : databaseDirectories) {
            String initialSql = readSql("src/main/resources/transactionalmq/db/migration/" + databaseDirectory
                    + "/V1__init.sql").toLowerCase();
            String upgradeSql = readSql("src/main/resources/transactionalmq/db/migration/" + databaseDirectory
                    + "/V2__consume_retry.sql").toLowerCase();
            Assert.assertFalse(initialSql.contains("original_message_id"));
            this.assertConsumeRetrySchema(upgradeSql);
            this.assertHistoryRetryUniqueKey(upgradeSql);
        }
    }

    /**
     * 验证 SQL 文本包含消费重试结构。
     *
     * @param sql SQL 文本
     */
    private void assertConsumeRetrySchema(String sql) {
        assertContains(sql, "original_message_id");
        assertContains(sql, "retry_count");
        assertContains(sql, "consumer_code");
        assertContains(sql, "last_error");
        assertContains(sql, "uk_txn_message_original_retry");
    }

    /**
     * 验证历史表具有消费重试唯一键。
     *
     * @param sql SQL 文本
     */
    private void assertHistoryRetryUniqueKey(String sql) {
        Assert.assertTrue("Missing history consume retry unique key",
                sql.contains("uk_txn_message_history_original_retry")
                        || sql.contains("uk_txn_msg_his_orig_retry"));
    }

    private String readSql(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    private void assertContains(String text, String expected) {
        Assert.assertTrue("Missing sql fragment: " + expected, text.contains(expected));
    }
}
