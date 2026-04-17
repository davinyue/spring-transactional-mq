package org.rdlinux.transactionalmq.common.id;

import org.junit.Assert;
import org.junit.Test;

/**
 * ObjectId 生成器测试
 */
public class ObjectIdGeneratorTest {

    /**
     * 验证生成的 id 为 ObjectId 的 24 位十六进制字符串
     */
    @Test
    public void should_generate_24_length_hex_object_id() {
        String id = ObjectIdGenerator.generate();

        Assert.assertNotNull(id);
        Assert.assertEquals(24, id.length());
        Assert.assertTrue(id.matches("[0-9a-f]{24}"));
    }
}
