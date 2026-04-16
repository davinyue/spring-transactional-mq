package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.rdlinux.ezmybatis.core.EzQuery;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.sqlstruct.Select;
import org.rdlinux.ezmybatis.core.sqlstruct.table.EntityTable;
import org.rdlinux.transactionalmq.common.enums.ConsumeStatus;
import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageHistoryEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 ez-mybatis 的消费消息仓储实现。
 */
@Repository
public class EzMybatisConsumedMessageRepository implements ConsumedMessageRepository {

    private static final EntityTable TABLE = EntityTable.of(ConsumedMessageEntity.class);

    @Resource
    private EzDao ezDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveIfAbsent(ConsumedMessageRecord record) {
        ConsumedMessageEntity entity = toEntity(record);
        try {
            this.ezDao.insert(entity);
            applyGeneratedIdentity(record, entity);
            this.ezDao.insert(toHistoryEntity(record, new Date()));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public List<ConsumedMessageRecord> findArchiveCandidates(Date archiveBefore, int limit) {
        if (archiveBefore == null || limit < 1) {
            return Collections.emptyList();
        }
        EzQuery<ConsumedMessageEntity> query = EzQuery.builder(ConsumedMessageEntity.class)
            .from(TABLE)
            .select(Select.EzSelectBuilder::addAll)
            .where(w -> w
                .add(TABLE.field(ConsumedMessageEntity.Fields.consumeStatus), ConsumeStatus.SUCCESS)
                .add(TABLE.field(ConsumedMessageEntity.Fields.consumeTime).le(archiveBefore)))
            .orderBy(o -> o.add(TABLE.field(ConsumedMessageEntity.Fields.consumeTime)))
            .page(1, limit)
            .build();
        List<ConsumedMessageEntity> entities = this.ezDao.query(query);
        List<ConsumedMessageRecord> records = new ArrayList<ConsumedMessageRecord>(entities.size());
        for (ConsumedMessageEntity entity : entities) {
            records.add(toRecord(entity));
        }
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archive(List<ConsumedMessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        List<ConsumedMessageEntity> sourceEntities = new ArrayList<ConsumedMessageEntity>(records.size());
        for (ConsumedMessageRecord record : records) {
            ConsumedMessageEntity source = toEntity(record);
            sourceEntities.add(source);
        }
        this.ezDao.batchDeleteByTable(TABLE, sourceEntities);
        return records.size();
    }

    private ConsumedMessageEntity toEntity(ConsumedMessageRecord record) {
        if (record == null) {
            return null;
        }
        Date now = new Date();
        ConsumedMessageEntity entity = new ConsumedMessageEntity();
        entity.setId(TransactionalMessageEntityMapper.resolvePrimaryKey(record.getId()));
        entity.setCreateTime(defaultDate(record.getCreateTime(), now));
        entity.setUpdateTime(defaultDate(record.getUpdateTime(), entity.getCreateTime()));
        entity.setMessageKey(record.getMessageKey());
        entity.setConsumerCode(record.getConsumerCode());
        entity.setBizKey(record.getBizKey());
        entity.setConsumeStatus(defaultConsumeStatus(record.getConsumeStatus()));
        entity.setConsumeTime(defaultDate(record.getConsumeTime(), now));
        return entity;
    }

    private ConsumedMessageHistoryEntity toHistoryEntity(ConsumedMessageRecord record, Date archiveTime) {
        Date now = new Date();
        ConsumedMessageHistoryEntity entity = new ConsumedMessageHistoryEntity();
        entity.setId(TransactionalMessageEntityMapper.resolvePrimaryKey(record.getId()));
        entity.setCreateTime(defaultDate(record.getCreateTime(), now));
        entity.setUpdateTime(defaultDate(record.getUpdateTime(), entity.getCreateTime()));
        entity.setMessageKey(record.getMessageKey());
        entity.setConsumerCode(record.getConsumerCode());
        entity.setBizKey(record.getBizKey());
        entity.setConsumeStatus(defaultConsumeStatus(record.getConsumeStatus()));
        entity.setConsumeTime(defaultDate(record.getConsumeTime(), now));
        entity.setArchiveTime(defaultDate(archiveTime, now));
        return entity;
    }

    private ConsumedMessageRecord toRecord(ConsumedMessageEntity entity) {
        ConsumedMessageRecord record = new ConsumedMessageRecord();
        record.setId(entity.getId());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        record.setMessageKey(entity.getMessageKey());
        record.setConsumerCode(entity.getConsumerCode());
        record.setBizKey(entity.getBizKey());
        record.setConsumeStatus(entity.getConsumeStatus());
        record.setConsumeTime(entity.getConsumeTime());
        return record;
    }

    private void applyGeneratedIdentity(ConsumedMessageRecord record, ConsumedMessageEntity entity) {
        record.setId(entity.getId());
    }

    private Date defaultDate(Date date, Date defaultDate) {
        return date == null ? defaultDate : date;
    }

    private ConsumeStatus defaultConsumeStatus(ConsumeStatus consumeStatus) {
        return consumeStatus == null ? ConsumeStatus.SUCCESS : consumeStatus;
    }
}
