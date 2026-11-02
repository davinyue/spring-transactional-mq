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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 基于 ez-mybatis 的消费消息仓储实现
 */
@Repository
public class EzMybatisConsumedMessageRepository implements ConsumedMessageRepository {

    /**
     * 已消费消息实体表
     */
    private static final EntityTable TABLE = EntityTable.of(ConsumedMessageEntity.class);
    /**
     * 已消费消息历史表。
     */
    private static final EntityTable HISTORY_TABLE = EntityTable.of(ConsumedMessageHistoryEntity.class);

    /**
     * ez-mybatis 数据访问对象
     */
    @Resource
    private EzDao ezDao;

    /**
     * 保存首次消费记录及其历史记录
     *
     * @param record 消费记录
     * @return 是否首次保存成功
     */
    @Override
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public boolean saveIfAbsent(ConsumedMessageRecord record) {
        ConsumedMessageEntity entity = this.toEntity(record);
        this.ezDao.insert(entity);
        this.applyGeneratedIdentity(record, entity);
        this.ezDao.insert(this.toHistoryEntity(record, new Date()));
        return true;
    }

    /**
     * 核验消费主表或历史表中是否存在同一消息和消费者。
     *
     * @param record 消费记录身份
     * @return 是否已存在匹配记录
     */
    @Override
    public boolean exists(ConsumedMessageRecord record) {
        EzQuery<ConsumedMessageEntity> query = EzQuery.builder(ConsumedMessageEntity.class)
                .from(TABLE).select(Select.EzSelectBuilder::addAll)
                .where(w -> w.add(TABLE.field(BaseEntity.Fields.id).eq(record.getId()))
                        .add(TABLE.field(ConsumedMessageEntity.Fields.consumerCode).eq(record.getConsumerCode())))
                .page(1, 1).build();
        if (!this.ezDao.query(query).isEmpty()) {
            return true;
        }
        EzQuery<ConsumedMessageHistoryEntity> historyQuery = EzQuery.builder(ConsumedMessageHistoryEntity.class)
                .from(HISTORY_TABLE).select(Select.EzSelectBuilder::addAll)
                .where(w -> w.add(HISTORY_TABLE.field(BaseEntity.Fields.id).eq(record.getId()))
                        .add(HISTORY_TABLE.field(ConsumedMessageHistoryEntity.Fields.consumerCode)
                                .eq(record.getConsumerCode())))
                .page(1, 1).build();
        return !this.ezDao.query(historyQuery).isEmpty();
    }

    /**
     * 查询待归档的消费记录
     *
     * @param archiveBefore 归档截止时间
     * @param limit         最大查询条数
     * @return 待归档消费记录
     */
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

    /**
     * 归档并删除消费主表记录
     *
     * @param records 待归档消费记录
     * @return 删除记录数
     */
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

    /**
     * 将消费记录转换为实体
     *
     * @param record 消费记录
     * @return 消费记录实体
     */
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

    /**
     * 将消费记录转换为历史实体
     *
     * @param record      消费记录
     * @param archiveTime 归档时间
     * @return 消费历史实体
     */
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

    /**
     * 将消费实体转换为记录
     *
     * @param entity 消费记录实体
     * @return 消费记录
     */
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

    /**
     * 将实体生成的主键回写到记录
     *
     * @param record 消费记录
     * @param entity 消费记录实体
     */
    private void applyGeneratedIdentity(ConsumedMessageRecord record, ConsumedMessageEntity entity) {
        record.setId(entity.getId());
    }

    /**
     * 使用默认时间填充空值
     *
     * @param date        原始时间
     * @param defaultDate 默认时间
     * @return 原始时间或默认时间
     */
    private Date defaultDate(Date date, Date defaultDate) {
        return date == null ? defaultDate : date;
    }

    /**
     * 使用默认消费状态填充空值
     *
     * @param consumeStatus 原始消费状态
     * @return 原始消费状态或成功状态
     */
    private ConsumeStatus defaultConsumeStatus(ConsumeStatus consumeStatus) {
        return consumeStatus == null ? ConsumeStatus.SUCCESS : consumeStatus;
    }
}
