package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;

public class TransactionalMessageEntityTest {

    @Test
    public void should_extend_base_entity_and_keep_key_fields() {
        TransactionalMessageEntity entity = new TransactionalMessageEntity();
        Date createTime = new Date(1710000000000L);

        entity.setId("txn-1")
            .setCreateTime(createTime)
            .setMessageKey("msg-key")
            .setProducerCode("producer-1")
            .setMqType(MqType.RABBITMQ)
            .setDestination("order.exchange")
            .setPayloadText("{\"id\":1}")
            .setHeadersJson("{\"traceId\":\"t-1\"}")
            .setBizKey("biz-1")
            .setMessageStatus(MessageStatus.INIT)
            .setNextDispatchTime(new Date(1710003600000L));

        Assert.assertTrue(entity instanceof BaseEntity);
        Assert.assertEquals("txn-1", entity.getId());
        Assert.assertSame(createTime, entity.getCreateTime());
        Assert.assertEquals("msg-key", entity.getMessageKey());
        Assert.assertEquals("producer-1", entity.getProducerCode());
        Assert.assertEquals(MqType.RABBITMQ, entity.getMqType());
        Assert.assertEquals("order.exchange", entity.getDestination());
        Assert.assertEquals("{\"id\":1}", entity.getPayloadText());
        Assert.assertEquals("{\"traceId\":\"t-1\"}", entity.getHeadersJson());
        Assert.assertEquals("biz-1", entity.getBizKey());
        Assert.assertEquals(MessageStatus.INIT, entity.getMessageStatus());
        Assert.assertNotNull(entity.getNextDispatchTime());
    }

}
