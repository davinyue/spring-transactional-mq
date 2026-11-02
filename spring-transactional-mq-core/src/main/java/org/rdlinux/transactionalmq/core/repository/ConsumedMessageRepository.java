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
     * 判断同一消费者是否已处理原始消息。
     *
     * @param record 消费记录身份
     * @return 是否存在匹配的消费记录
     */
    default boolean exists(ConsumedMessageRecord record) {
        return false;
    }

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
