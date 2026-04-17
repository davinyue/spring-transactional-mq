package org.rdlinux.transactionalmq.core.repository;

import java.util.List;

import org.rdlinux.transactionalmq.core.model.MessageSendLogRecord;

/**
 * 发送日志仓储 SPI
 */
public interface MessageSendLogRepository {

    /**
     * 保存发送日志
     *
     * @param record 发送日志记录
     * @return 持久化后的发送日志
     */
    MessageSendLogRecord save(MessageSendLogRecord record);

    /**
     * 获取待重试的发送日志
     *
     * @param limit 读取条数
     * @return 待重试日志
     */
    List<MessageSendLogRecord> findRetryCandidates(int limit);
}
