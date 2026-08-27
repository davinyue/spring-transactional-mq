package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import org.rdlinux.ezmybatis.core.EzDelete;
import org.rdlinux.ezmybatis.core.EzQuery;
import org.rdlinux.ezmybatis.core.EzUpdate;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.sqlstruct.Select;
import org.rdlinux.ezmybatis.core.sqlstruct.table.EntityTable;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.id.ObjectIdGenerator;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageHistoryEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 基于 ez-mybatis 的事务消息仓储实现
 */
@Repository
public class EzMybatisTransactionalMessageRepository implements TransactionalMessageRepository {

    /**
     * 派发锁持有时间（毫秒）
     */
    private static final long DISPATCH_LOCK_MILLIS = 5 * 60 * 1000L;
    /**
     * 派发失败后的重试间隔（毫秒）
     */
    private static final long RETRY_DELAY_MILLIS = 60 * 1000L;
    /**
     * 事务消息实体表
     */
    private static final EntityTable TABLE = EntityTable.of(TransactionalMessageEntity.class);
    /**
     * 当前实例的派发所有者标识
     */
    private static final String DISPATCH_OWNER = ObjectIdGenerator.generate();

    /**
     * ez-mybatis 数据访问对象
     */
    @Resource
    private EzDao ezDao;

    /**
     * 保存事务消息记录
     *
     * @param record 事务消息记录
     * @return 保存后的事务消息记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionalMessageRecord save(TransactionalMessageRecord record) {
        TransactionalMessageEntity entity = TransactionalMessageEntityMapper.toEntity(record);
        this.ezDao.insert(entity);
        this.applyGeneratedIdentity(record, entity);
        return TransactionalMessageEntityMapper.toRecord(entity);
    }

    /**
     * 保存消费重试消息记录
     *
     * @param record 消费重试消息记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void saveConsumeRetry(TransactionalMessageRecord record) {
        TransactionalMessageEntity entity = TransactionalMessageEntityMapper.toEntity(record);
        this.ezDao.insert(entity);
        this.applyGeneratedIdentity(record, entity);
    }

    /**
     * 将停止重试的死信记录直接写入事务消息历史表
     *
     * @param record 死信记录
     */
    /**
     * 查询待派发的事务消息
     *
     * @param limit 最大查询条数
     * @return 待派发事务消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void saveDeadConsumeRetry(TransactionalMessageRecord record) {
        TransactionalMessageHistoryEntity entity = TransactionalMessageEntityMapper.toHistoryEntity(record);
        this.ezDao.insert(entity);
        this.applyGeneratedIdentity(record, entity);
    }

    /**
     * 领取一条待派发消息并设置派发锁
     *
     * @param record 待领取消息
     * @return 领取成功后的消息，领取失败时返回 null
     */
    @Override
    public List<TransactionalMessageRecord> findDispatchCandidates(int limit) {
        if (limit < 1) {
            return Collections.emptyList();
        }
        Date now = new Date();
        EzQuery<TransactionalMessageEntity> query = EzQuery.builder(TransactionalMessageEntity.class)
                .from(TABLE)
                .select(Select.EzSelectBuilder::addAll)
                .where(w -> w
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus).in(MessageStatus.INIT,
                                MessageStatus.RETRYING, MessageStatus.SENDING))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.nextDispatchTime).le(now)))
                .orderBy(o -> o
                        .add(TABLE.field(TransactionalMessageEntity.Fields.nextDispatchTime))
                        .add(TABLE.field(BaseEntity.Fields.id)))
                .page(1, limit)
                .build();
        List<TransactionalMessageEntity> entities = this.ezDao.query(query);
        List<TransactionalMessageRecord> records = new ArrayList<>(entities.size());
        for (TransactionalMessageEntity entity : entities) {
            records.add(TransactionalMessageEntityMapper.toRecord(entity));
        }
        return records;
    }

    /**
     * 标记消息派发成功
     *
     * @param records 已成功派发的消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionalMessageRecord claimDispatchMessage(TransactionalMessageRecord record) {
        Date now = new Date();
        TransactionalMessageEntity entity = TransactionalMessageEntityMapper.toEntity(record);
        entity.setMessageStatus(MessageStatus.SENDING);
        entity.setUpdateTime(now);
        entity.setNextDispatchTime(new Date(now.getTime() + DISPATCH_LOCK_MILLIS));
        entity.setDispatchOwner(DISPATCH_OWNER);
        entity.setDispatchToken(ObjectIdGenerator.generate());
        entity.setDispatchExpireTime(entity.getNextDispatchTime());
        int updated = this.ezDao.ezUpdate(EzUpdate.update(TABLE)
                .set(s -> s
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus).set(entity.getMessageStatus()))
                        .add(TABLE.field(BaseEntity.Fields.updateTime).set(entity.getUpdateTime()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.nextDispatchTime).set(entity.getNextDispatchTime()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.dispatchOwner).set(entity.getDispatchOwner()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.dispatchToken).set(entity.getDispatchToken()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.dispatchExpireTime)
                                .set(entity.getDispatchExpireTime())))
                .where(w -> w
                        .add(TABLE.field(BaseEntity.Fields.id).eq(entity.getId()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus).in(MessageStatus.INIT,
                                MessageStatus.RETRYING, MessageStatus.SENDING))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.nextDispatchTime).le(now)))
                .build());
        return updated > 0 ? TransactionalMessageEntityMapper.toRecord(entity) : null;
    }

    /**
     * 标记消息派发成功
     *
     * @param records 已成功派发的消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDispatchSuccess(List<TransactionalMessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (TransactionalMessageRecord record : records) {
            this.markDispatchResult(record, MessageStatus.SUCCESS, null);
        }
    }

    /**
     * 标记消息派发失败并安排重试
     *
     * @param records 派发失败的消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDispatchFailed(List<TransactionalMessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Date nextDispatchTime = new Date(System.currentTimeMillis() + RETRY_DELAY_MILLIS);
        for (TransactionalMessageRecord record : records) {
            this.markDispatchResult(record, MessageStatus.RETRYING, nextDispatchTime);
        }
    }

    /**
     * 更新消息派发结果
     *
     * @param record 消息记录
     * @param messageStatus 消息状态
     * @param nextDispatchTime 下一次派发时间
     */
    private void markDispatchResult(TransactionalMessageRecord record, MessageStatus messageStatus,
                                    Date nextDispatchTime) {
        Date now = new Date();
        EzUpdate.EzUpdateBuilder builder = EzUpdate.update(TABLE)
                .set(s -> {
                    s.add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus).set(messageStatus))
                            .add(TABLE.field(BaseEntity.Fields.updateTime).set(now))
                            .add(TABLE.field(TransactionalMessageEntity.Fields.dispatchOwner).setToNull())
                            .add(TABLE.field(TransactionalMessageEntity.Fields.dispatchToken).setToNull())
                            .add(TABLE.field(TransactionalMessageEntity.Fields.dispatchExpireTime).setToNull());
                    if (nextDispatchTime == null) {
                        s.add(TABLE.field(TransactionalMessageEntity.Fields.nextDispatchTime).setToNull());
                    } else {
                        s.add(TABLE.field(TransactionalMessageEntity.Fields.nextDispatchTime).set(nextDispatchTime));
                    }
                })
                .where(w -> w
                        .add(TABLE.field(BaseEntity.Fields.id).eq(record.getId()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus).eq(MessageStatus.SENDING))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.dispatchToken).eq(record.getDispatchToken())));
        this.ezDao.ezUpdate(builder.build());
    }

    /**
     * 查询待清理的成功消息
     *
     * @param cleanupBefore 清理截止时间
     * @param limit 最大查询条数
     * @return 待清理成功消息
     */
    @Override
    public List<TransactionalMessageRecord> findSuccessCleanupCandidates(Date cleanupBefore, int limit) {
        if (cleanupBefore == null || limit < 1) {
            return Collections.emptyList();
        }
        EzQuery<TransactionalMessageEntity> query = EzQuery.builder(TransactionalMessageEntity.class)
                .from(TABLE)
                .select(Select.EzSelectBuilder::addAll)
                .where(w -> w
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus), MessageStatus.SUCCESS)
                        .add(TABLE.field(BaseEntity.Fields.updateTime).le(cleanupBefore)))
                .page(1, limit)
                .build();
        List<TransactionalMessageEntity> entities = this.ezDao.query(query);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransactionalMessageRecord> records = new ArrayList<>(entities.size());
        for (TransactionalMessageEntity entity : entities) {
            records.add(TransactionalMessageEntityMapper.toRecord(entity));
        }
        return records;
    }

    /**
     * 归档并删除一条成功消息
     *
     * @param record 成功消息记录
     * @param cleanupBefore 清理截止时间
     * @return 删除记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveSuccessMessage(TransactionalMessageRecord record, Date cleanupBefore) {
        if (record == null || cleanupBefore == null) {
            return 0;
        }
        TransactionalMessageEntity entity = TransactionalMessageEntityMapper.toEntity(record);
        if (this.claimArchiveMessage(entity, cleanupBefore) < 1) {
            return 0;
        }
        entity.setMessageStatus(MessageStatus.SUCCESS);
        this.insertHistoryIfAbsent(TransactionalMessageEntityMapper.toRecord(entity));
        return this.deleteArchivedMessage(entity);
    }

    /**
     * 将成功消息标记为归档中
     *
     * @param entity 事务消息实体
     * @param cleanupBefore 清理截止时间
     * @return 更新记录数
     */
    private int claimArchiveMessage(TransactionalMessageEntity entity, Date cleanupBefore) {
        return this.ezDao.ezUpdate(EzUpdate.update(TABLE)
                .set(s -> s
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus).set(MessageStatus.ARCHIVING))
                        .add(TABLE.field(BaseEntity.Fields.updateTime).set(new Date())))
                .where(w -> w
                        .add(TABLE.field(BaseEntity.Fields.id).eq(entity.getId()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus), MessageStatus.SUCCESS)
                        .add(TABLE.field(BaseEntity.Fields.updateTime).le(cleanupBefore)))
                .build());
    }

    /**
     * 插入历史记录，重复记录直接忽略
     *
     * @param record 事务消息记录
     */
    private void insertHistoryIfAbsent(TransactionalMessageRecord record) {
        try {
            this.ezDao.insert(TransactionalMessageEntityMapper.toHistoryEntity(record));
        } catch (DuplicateKeyException ex) {
            // 其他实例或历史重试可能已经写入历史表，继续删除主表即可
        }
    }

    /**
     * 删除已归档的主表消息
     *
     * @param entity 已归档事务消息实体
     * @return 删除记录数
     */
    private int deleteArchivedMessage(TransactionalMessageEntity entity) {
        return this.ezDao.ezDelete(EzDelete.delete(TABLE)
                .where(w -> w
                        .add(TABLE.field(BaseEntity.Fields.id).eq(entity.getId()))
                        .add(TABLE.field(TransactionalMessageEntity.Fields.messageStatus), MessageStatus.ARCHIVING))
                .build());
    }

    /**
     * 将实体生成的主键回写到记录
     *
     * @param record 事务消息记录
     * @param entity 事务消息实体
     */
    private void applyGeneratedIdentity(TransactionalMessageRecord record, TransactionalMessageEntity entity) {
        record.setId(entity.getId());
    }

}
