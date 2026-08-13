package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务消息仓储实现风格测试
 */
public class EzMybatisTransactionalMessageRepositoryStyleTest {

    /**
     * 验证仓储实现不再使用手写 SQL
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
     * 验证消费重试插入使用独立事务，使唯一键冲突可完整回滚后再被上层处理。
     *
     * @throws Exception 方法反射失败
     */
    @Test
    public void consumeRetrySaveShouldUseRequiresNewTransaction() throws Exception {
        Method method = EzMybatisTransactionalMessageRepository.class.getMethod("saveConsumeRetry",
                TransactionalMessageRecord.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        Assert.assertNotNull(transactional);
        Assert.assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

}
