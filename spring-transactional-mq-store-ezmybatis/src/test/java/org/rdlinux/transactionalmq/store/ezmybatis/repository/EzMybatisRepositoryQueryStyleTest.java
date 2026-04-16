package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

/**
 * ez-mybatis 仓储查询风格测试。
 */
public class EzMybatisRepositoryQueryStyleTest {

    /**
     * 验证仓储查询分页统一使用 page。
     *
     * @throws Exception 读取源码失败
     */
    @Test
    public void repositoriesShouldUsePageInsteadOfLimit() throws Exception {
        File repositoryDir = new File("src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository");
        Assert.assertFalse(contains(repositoryDir, ".limit(limit)"));
    }

    private boolean contains(File file, String pattern) throws Exception {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return false;
            }
            for (File child : children) {
                if (contains(child, pattern)) {
                    return true;
                }
            }
            return false;
        }
        if (!file.getName().endsWith(".java")) {
            return false;
        }
        String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return source.contains(pattern);
    }
}
