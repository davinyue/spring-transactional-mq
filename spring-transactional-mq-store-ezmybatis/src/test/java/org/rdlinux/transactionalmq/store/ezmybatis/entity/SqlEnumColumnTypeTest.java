package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

public class SqlEnumColumnTypeTest {

    @Test
    public void mysqlSqlShouldUseIntColumnsForEnums() throws Exception {
        String sql = readSql("src/main/resources/sql/MYSQL.sql");

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
        assertContains(readSql("src/main/resources/sql/POSTGRE_SQL.sql"), "mq_type INTEGER NOT NULL");
        assertContains(readSql("src/main/resources/sql/POSTGRE_SQL.sql"), "message_status INTEGER NOT NULL");
        assertContains(readSql("src/main/resources/sql/POSTGRE_SQL.sql"), "consume_status INTEGER NOT NULL");
        assertContains(readSql("src/main/resources/sql/POSTGRE_SQL.sql"), "send_status INTEGER NOT NULL");

        assertContains(readSql("src/main/resources/sql/ORACLE.sql"), "mq_type NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/sql/ORACLE.sql"), "message_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/sql/ORACLE.sql"), "consume_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/sql/ORACLE.sql"), "send_status NUMBER(10) NOT NULL");

        assertContains(readSql("src/main/resources/sql/DM.sql"), "mq_type NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/sql/DM.sql"), "message_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/sql/DM.sql"), "consume_status NUMBER(10) NOT NULL");
        assertContains(readSql("src/main/resources/sql/DM.sql"), "send_status NUMBER(10) NOT NULL");

        assertContains(readSql("src/main/resources/sql/SQL_SERVER.sql"), "mq_type INT NOT NULL");
        assertContains(readSql("src/main/resources/sql/SQL_SERVER.sql"), "message_status INT NOT NULL");
        assertContains(readSql("src/main/resources/sql/SQL_SERVER.sql"), "consume_status INT NOT NULL");
        assertContains(readSql("src/main/resources/sql/SQL_SERVER.sql"), "send_status INT NOT NULL");
    }

    /**
     * 验证五种数据库的完整建表与升级脚本包含消费重试字段和唯一键。
     *
     * @throws Exception SQL 文件读取失败
     */
    @Test
    public void allDatabaseSqlShouldContainConsumeRetrySchema() throws Exception {
        String[] databaseNames = {"MYSQL", "ORACLE", "DM", "POSTGRE_SQL", "SQL_SERVER"};
        for (String databaseName : databaseNames) {
            String createSql = readSql("src/main/resources/sql/" + databaseName + ".sql").toLowerCase();
            String upgradeSql = readSql("src/main/resources/sql/upgrade/20260813_" + databaseName
                    + "_CONSUME_RETRY.sql").toLowerCase();
            this.assertConsumeRetrySchema(createSql);
            this.assertConsumeRetrySchema(upgradeSql);
            this.assertHistoryRetryUniqueKey(createSql);
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
