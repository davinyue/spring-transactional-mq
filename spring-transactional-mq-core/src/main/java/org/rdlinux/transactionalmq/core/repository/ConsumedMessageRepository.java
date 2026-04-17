package org.rdlinux.transactionalmq.core.repository;

import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;

import java.util.Date;
import java.util.List;

/**
 * 已消费消息仓储 SPI
 */
public interface ConsumedMessageRepository {

    /**
     * 原子保存消费记录
     *
     * @param record 消费记录
     * @return 是否首次保存成功
     */
    boolean saveIfAbsent(ConsumedMessageRecord record);

    /**
     * 获取待归档消费记录
     *
     * @param archiveBefore 归档阈值时间
     * @param limit         读取条数
     * @return 待归档记录
     */
    List<ConsumedMessageRecord> findArchiveCandidates(Date archiveBefore, int limit);

    /**
     * 归档消费记录
     *
     * @param records 待归档记录
     * @return 归档数量
     */
    int archive(List<ConsumedMessageRecord> records);
}
