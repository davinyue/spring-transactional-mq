package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageEntity;

public class TransactionalMessageEntityMapperTest {

    @Test
    public void should_map_record_to_entity_and_back() {
        TransactionalMessageRecord record = new TransactionalMessageRecord();
        record.setId("msg-1");
        record.setMessageKey("msg-key");
        record.setProducerCode("producer-1");
        record.setMqType(MqType.RABBITMQ);
        record.setDestination("order.exchange");
        record.setPayloadText("{\"id\":1}");
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("traceId", "t-1");
        record.setHeaders(headers);
        record.setBizKey("biz-1");
        record.setMessageStatus(MessageStatus.INIT);
        record.setNextDispatchTime(new Date(1710003600000L));
        record.setDispatchOwner("node-1");
        record.setDispatchToken("token-1");
        record.setDispatchExpireTime(new Date(1710003900000L));

        TransactionalMessageEntity entity = TransactionalMessageEntityMapper.toEntity(record);

        Assert.assertEquals("msg-1", entity.getId());
        Assert.assertEquals("msg-key", entity.getMessageKey());
        Assert.assertEquals("producer-1", entity.getProducerCode());
        Assert.assertEquals(MqType.RABBITMQ, entity.getMqType());
        Assert.assertEquals("order.exchange", entity.getDestination());
        Assert.assertEquals("{\"id\":1}", entity.getPayloadText());
        Assert.assertEquals("{\"traceId\":\"t-1\"}", entity.getHeadersJson());
        Assert.assertEquals("biz-1", entity.getBizKey());
        Assert.assertEquals(MessageStatus.INIT, entity.getMessageStatus());
        Assert.assertEquals("node-1", entity.getDispatchOwner());
        Assert.assertEquals("token-1", entity.getDispatchToken());
        Assert.assertNotNull(entity.getDispatchExpireTime());

        TransactionalMessageRecord mappedBack = TransactionalMessageEntityMapper.toRecord(entity);
        Assert.assertEquals("msg-1", mappedBack.getId());
        Assert.assertEquals("msg-key", mappedBack.getMessageKey());
        Assert.assertEquals("producer-1", mappedBack.getProducerCode());
        Assert.assertEquals(MqType.RABBITMQ, mappedBack.getMqType());
        Assert.assertEquals("order.exchange", mappedBack.getDestination());
        Assert.assertEquals("{\"id\":1}", mappedBack.getPayloadText());
        Assert.assertEquals("t-1", mappedBack.getHeaders().get("traceId"));
        Assert.assertEquals("biz-1", mappedBack.getBizKey());
        Assert.assertEquals(MessageStatus.INIT, mappedBack.getMessageStatus());
        Assert.assertNotNull(mappedBack.getNextDispatchTime());
        Assert.assertEquals("node-1", mappedBack.getDispatchOwner());
        Assert.assertEquals("token-1", mappedBack.getDispatchToken());
        Assert.assertNotNull(mappedBack.getDispatchExpireTime());
    }

    @Test
    public void should_generate_primary_key_when_id_missing() {
        TransactionalMessageRecord record = new TransactionalMessageRecord();
        record.setMessageKey("msg-key-2");

        TransactionalMessageEntity entity = TransactionalMessageEntityMapper.toEntity(record);

        Assert.assertNotNull(entity.getId());
        Assert.assertNotNull(entity.getCreateTime());
        Assert.assertNotNull(entity.getUpdateTime());
        Assert.assertEquals(24, entity.getId().length());
    }
}
