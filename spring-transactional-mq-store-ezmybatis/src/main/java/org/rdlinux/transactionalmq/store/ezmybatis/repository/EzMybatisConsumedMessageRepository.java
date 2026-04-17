package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import org.rdlinux.ezmybatis.core.EzDelete;
import org.rdlinux.ezmybatis.core.EzQuery;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.sqlstruct.Select;
import org.rdlinux.ezmybatis.core.sqlstruct.table.EntityTable;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.ConsumeStatus;
import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageHistoryEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 基于 ez-mybatis 的消费消息仓储实现
 */
@Repository
public class EzMybatisConsumedMessageRepository implements ConsumedMessageRepository {

    private static final EntityTable TABLE = EntityTable.of(ConsumedMessageEntity.class);

    @Resource
    private EzDao ezDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveIfAbsent(ConsumedMessageRecord record) {
        ConsumedMessageEntity entity = this.toEntity(record);
        try {
            this.ezDao.insert(entity);
            this.applyGeneratedIdentity(record, entity);
            this.ezDao.insert(this.toHistoryEntity(record, new Date()));
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
                .page(1, limit)
                .build();
        List<ConsumedMessageEntity> entities = this.ezDao.query(query);
        List<ConsumedMessageRecord> records = new ArrayList<>(entities.size());
        for (ConsumedMessageEntity entity : entities) {
            records.add(this.toRecord(entity));
        }
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archive(List<ConsumedMessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        List<ConsumedMessageRecord> orderedRecords = new ArrayList<>(records);
        orderedRecords.sort(Comparator.comparing(BaseEntity::getId));
        int deleted = 0;
        for (ConsumedMessageRecord record : orderedRecords) {
            deleted += this.ezDao.ezDelete(EzDelete.delete(TABLE)
                    .where(w -> w
                            .add(TABLE.field(BaseEntity.Fields.id).eq(record.getId()))
                            .add(TABLE.field(ConsumedMessageEntity.Fields.consumeStatus).eq(ConsumeStatus.SUCCESS)))
                    .build());
        }
        return deleted;
    }

    private ConsumedMessageEntity toEntity(ConsumedMessageRecord record) {
        if (record == null) {
            return null;
        }
        Date now = new Date();
        ConsumedMessageEntity entity = new ConsumedMessageEntity();
        entity.setId(TransactionalMessageEntityMapper.resolvePrimaryKey(record.getId()));
        entity.setCreateTime(this.defaultDate(record.getCreateTime(), now));
        entity.setUpdateTime(this.defaultDate(record.getUpdateTime(), entity.getCreateTime()));
        entity.setMessageKey(record.getMessageKey());
        entity.setConsumerCode(record.getConsumerCode());
        entity.setBizKey(record.getBizKey());
        entity.setConsumeStatus(this.defaultConsumeStatus(record.getConsumeStatus()));
        entity.setConsumeTime(this.defaultDate(record.getConsumeTime(), now));
        return entity;
    }

    private ConsumedMessageHistoryEntity toHistoryEntity(ConsumedMessageRecord record, Date archiveTime) {
        Date now = new Date();
        ConsumedMessageHistoryEntity entity = new ConsumedMessageHistoryEntity();
        entity.setId(TransactionalMessageEntityMapper.resolvePrimaryKey(record.getId()));
        entity.setCreateTime(this.defaultDate(record.getCreateTime(), now));
        entity.setUpdateTime(this.defaultDate(record.getUpdateTime(), entity.getCreateTime()));
        entity.setMessageKey(record.getMessageKey());
        entity.setConsumerCode(record.getConsumerCode());
        entity.setBizKey(record.getBizKey());
        entity.setConsumeStatus(this.defaultConsumeStatus(record.getConsumeStatus()));
        entity.setConsumeTime(this.defaultDate(record.getConsumeTime(), now));
        entity.setArchiveTime(this.defaultDate(archiveTime, now));
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
