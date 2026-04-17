package org.rdlinux.transactionalmq.core.repository;

import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;

import java.util.Date;
import java.util.List;

/**
 * 事务消息仓储 SPI
 */
public interface TransactionalMessageRepository {

    /**
     * 保存事务消息记录
     *
     * @param record 消息记录
     * @return 持久化后的消息记录
     */
    TransactionalMessageRecord save(TransactionalMessageRecord record);

    /**
     * 查询待派发候选消息
     *
     * @param limit 查询条数
     * @return 待派发候选消息列表
     */
    List<TransactionalMessageRecord> findDispatchCandidates(int limit);

    /**
     * 尝试抢占单条待派发消息
     *
     * @param record 消息记录
     * @return 抢占成功后的消息记录；返回 {@code null} 表示已被其它线程抢占
     */
    TransactionalMessageRecord claimDispatchMessage(TransactionalMessageRecord record);

    /**
     * 批量标记消息派发成功
     *
     * @param records 消息记录
     */
    void markDispatchSuccess(List<TransactionalMessageRecord> records);

    /**
     * 批量标记消息派发失败
     *
     * @param records 消息记录
     */
    void markDispatchFailed(List<TransactionalMessageRecord> records);

    /**
     * 查询待归档的成功消息
     *
     * @param cleanupBefore 清理阈值时间
     * @param limit         最大读取条数
     * @return 成功消息候选列表
     */
    List<TransactionalMessageRecord> findSuccessCleanupCandidates(Date cleanupBefore, int limit);

    /**
     * 归档并删除单条已发送成功的事务消息主表记录
     *
     * @param record        成功消息记录
     * @param cleanupBefore 清理阈值时间
     * @return 归档并删除条数
     */
    int archiveSuccessMessage(TransactionalMessageRecord record, Date cleanupBefore);
}
