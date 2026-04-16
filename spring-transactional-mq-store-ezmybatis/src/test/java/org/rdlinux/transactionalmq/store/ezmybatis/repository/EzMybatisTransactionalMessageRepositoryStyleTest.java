package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

/**
 * 事务消息仓储实现风格测试。
 */
public class EzMybatisTransactionalMessageRepositoryStyleTest {

    /**
     * 验证仓储实现不再使用手写 SQL。
     *
     * @throws Exception 读取源码失败
     */
    @Test
    public void repositoryShouldNotUseRawSqlMethods() throws Exception {
        File sourceFile = new File("src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/"
            + "EzMybatisTransactionalMessageRepository.java");
        String source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);

        Assert.assertFalse(source.contains("updateBySql"));
        Assert.assertFalse(source.contains("deleteBySql"));
        Assert.assertFalse(source.contains("\"UPDATE "));
        Assert.assertFalse(source.contains("\"DELETE "));
        Assert.assertFalse(source.contains("TABLE.field(\""));
    }

    /**
     * 验证批量更新前的查询使用主键作为稳定排序兜底，降低多实例反向锁行导致死锁的概率。
     *
     * @throws Exception 读取源码失败
     */
    @Test
    public void repositoryShouldUseStableOrderForBatchClaims() throws Exception {
        File sourceFile = new File("src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/"
            + "EzMybatisTransactionalMessageRepository.java");
        String source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);

        Assert.assertTrue(source.contains(".add(TABLE.field(TransactionalMessageEntity.Fields.nextDispatchTime))\n"
            + "                .add(TABLE.field(BaseEntity.Fields.id))"));
        Assert.assertTrue(source.contains(".add(TABLE.field(BaseEntity.Fields.updateTime))\n"
            + "                .add(TABLE.field(BaseEntity.Fields.id))"));
    }
}
