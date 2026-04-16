package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.common.enums.SendStatus;
import org.rdlinux.transactionalmq.core.model.MessageSendLogRecord;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.MessageSendLogEntity;

public class EzMybatisMessageSendLogRepositoryTest {

    @Test
    public void should_keep_id_stable_in_mapping() {
        EzMybatisMessageSendLogRepository repository = new EzMybatisMessageSendLogRepository();

        MessageSendLogRecord record = new MessageSendLogRecord();
        record.setId("msg-9");
        record.setMessageKey("msg-key-9");
        record.setProducerCode("producer-9");
        record.setMqType(MqType.RABBITMQ);
        record.setSendStatus(SendStatus.FAILED);
        record.setRetryCount(2);

        MessageSendLogEntity entity = repository.toEntity(record);
        Assert.assertEquals("msg-9", entity.getId());
        Assert.assertNotNull(entity.getCreateTime());
        Assert.assertNotNull(entity.getUpdateTime());
        Assert.assertNotNull(entity.getLastSendTime());
        Assert.assertEquals(Integer.valueOf(2), entity.getRetryCount());

        MessageSendLogRecord mappedBack = repository.toRecord(entity);
        Assert.assertEquals(entity.getId(), mappedBack.getId());
    }
}
