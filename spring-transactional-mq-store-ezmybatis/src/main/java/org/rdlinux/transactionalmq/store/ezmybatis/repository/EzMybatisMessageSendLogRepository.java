package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.rdlinux.ezmybatis.core.EzQuery;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.sqlstruct.Select;
import org.rdlinux.ezmybatis.core.sqlstruct.table.EntityTable;
import org.rdlinux.transactionalmq.common.enums.SendStatus;
import org.rdlinux.transactionalmq.core.model.MessageSendLogRecord;
import org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.MessageSendLogEntity;
import org.springframework.stereotype.Repository;

/**
 * 基于 ez-mybatis 的发送日志仓储实现。
 */
@Repository
public class EzMybatisMessageSendLogRepository implements MessageSendLogRepository {

    private static final EntityTable TABLE = EntityTable.of(MessageSendLogEntity.class);

    @Resource
    private EzDao ezDao;

    @Override
    public MessageSendLogRecord save(MessageSendLogRecord record) {
        MessageSendLogEntity entity = toEntity(record);
        this.ezDao.insert(entity);
        record.setId(entity.getId());
        return toRecord(entity);
    }

    @Override
    public List<MessageSendLogRecord> findRetryCandidates(int limit) {
        if (limit < 1) {
            return Collections.emptyList();
        }
        EzQuery<MessageSendLogEntity> query = EzQuery.builder(MessageSendLogEntity.class)
            .from(TABLE)
            .select(Select.EzSelectBuilder::addAll)
            .where(w -> w.add(TABLE.field(MessageSendLogEntity.Fields.sendStatus), SendStatus.FAILED))
            .orderBy(o -> o.add(TABLE.field(MessageSendLogEntity.Fields.lastSendTime)))
            .page(1, limit)
            .build();
        List<MessageSendLogEntity> entities = this.ezDao.query(query);
        List<MessageSendLogRecord> records = new ArrayList<MessageSendLogRecord>(entities.size());
        for (MessageSendLogEntity entity : entities) {
            records.add(toRecord(entity));
        }
        return records;
    }

    MessageSendLogEntity toEntity(MessageSendLogRecord record) {
        Date now = new Date();
        MessageSendLogEntity entity = new MessageSendLogEntity();
        entity.setId(TransactionalMessageEntityMapper.resolvePrimaryKey(record.getId()));
        entity.setCreateTime(defaultDate(record.getCreateTime(), now));
        entity.setUpdateTime(defaultDate(record.getUpdateTime(), entity.getCreateTime()));
        entity.setMessageKey(record.getMessageKey());
        entity.setProducerCode(record.getProducerCode());
        entity.setMqType(record.getMqType());
        entity.setParentId(record.getParentId());
        entity.setRootId(record.getRootId());
        entity.setSendStatus(record.getSendStatus());
        entity.setRetryCount(record.getRetryCount());
        entity.setLastSendTime(defaultDate(record.getLastSendTime(), now));
        entity.setDescription(record.getDescription());
        if (entity.getSendStatus() == null) {
            entity.setSendStatus(SendStatus.INIT);
        }
        if (entity.getRetryCount() == null) {
            entity.setRetryCount(0);
        }
        return entity;
    }

    MessageSendLogRecord toRecord(MessageSendLogEntity entity) {
        MessageSendLogRecord record = new MessageSendLogRecord();
        record.setId(entity.getId());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        record.setMessageKey(entity.getMessageKey());
        record.setProducerCode(entity.getProducerCode());
        record.setMqType(entity.getMqType());
        record.setParentId(entity.getParentId());
        record.setRootId(entity.getRootId());
        record.setSendStatus(entity.getSendStatus());
        record.setRetryCount(entity.getRetryCount());
        record.setLastSendTime(entity.getLastSendTime());
        record.setDescription(entity.getDescription());
        return record;
    }

    private Date defaultDate(Date date, Date defaultDate) {
        return date == null ? defaultDate : date;
    }
}
