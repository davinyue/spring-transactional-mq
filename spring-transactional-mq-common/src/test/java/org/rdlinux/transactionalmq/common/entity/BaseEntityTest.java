package org.rdlinux.transactionalmq.common.entity;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.ConsumeStatus;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.common.enums.SendStatus;

public class BaseEntityTest {

    private static class TestEntity extends BaseEntity<TestEntity> {
    }

    @Test
    public void shouldStoreBaseFields() {
        TestEntity entity = new TestEntity();
        Date createTime = new Date(1710000000000L);
        Date updateTime = new Date(1710003600000L);

        entity.setId("msg-1");
        entity.setCreateTime(createTime);
        entity.setUpdateTime(updateTime);

        Assert.assertEquals("msg-1", entity.getId());
        Assert.assertSame(createTime, entity.getCreateTime());
        Assert.assertSame(updateTime, entity.getUpdateTime());
    }

    @Test
    public void shouldSupportChainSettersReturningSubType() {
        TestEntity entity = new TestEntity();
        Date createTime = new Date(1710000000000L);
        Date updateTime = new Date(1710003600000L);

        TestEntity chained = entity.setId("msg-2")
            .setCreateTime(createTime)
            .setUpdateTime(updateTime);

        Assert.assertSame(entity, chained);
        Assert.assertEquals("msg-2", chained.getId());
        Assert.assertSame(createTime, chained.getCreateTime());
        Assert.assertSame(updateTime, chained.getUpdateTime());
    }

    @Test
    public void shouldExposeEnumValues() {
        Assert.assertEquals("init", MessageStatus.INIT.getCode());
        Assert.assertEquals(MessageStatus.SUCCESS, MessageStatus.fromCode("success"));
        Assert.assertEquals(ConsumeStatus.CONSUMING, ConsumeStatus.fromCode("consuming"));
        Assert.assertEquals(SendStatus.FAILED, SendStatus.fromCode("failed"));
        Assert.assertEquals(MqType.RABBITMQ, MqType.fromCode("rabbitmq"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnknownEnumCode() {
        MessageStatus.fromCode("unknown");
    }
}
