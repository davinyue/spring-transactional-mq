package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

/**
 * 已消费消息仓储实现风格测试
 */
public class EzMybatisConsumedMessageRepositoryStyleTest {

    /**
     * 验证消费主表清理不再使用批量删除，并按 id 稳定排序后逐条删除，降低多实例死锁风险
     *
     * @throws Exception 读取源码失败
     */
    @Test
    public void repositoryShouldDeleteConsumedRecordsOneByOneAfterSortingById() throws Exception {
        File sourceFile = new File("src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/"
            + "EzMybatisConsumedMessageRepository.java");
        String source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);

        Assert.assertFalse(source.contains("batchDeleteByTable"));
        Assert.assertTrue(source.contains("orderedRecords.sort(Comparator.comparing(BaseEntity::getId))"));
        Assert.assertTrue(source.contains("this.ezDao.ezDelete(EzDelete.delete(TABLE)"));
    }
}
