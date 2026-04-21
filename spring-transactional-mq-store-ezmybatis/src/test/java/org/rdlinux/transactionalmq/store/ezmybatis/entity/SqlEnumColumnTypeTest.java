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

    private String readSql(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    private void assertContains(String text, String expected) {
        Assert.assertTrue("Missing sql fragment: " + expected, text.contains(expected));
    }
}
