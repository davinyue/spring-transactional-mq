package org.rdlinux.transactionalmq.core.service;

import java.util.Date;
import java.util.List;

import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;

/**
 * 已消费消息归档服务骨架。
 */
public class ConsumedMessageArchiveService {

    private final ConsumedMessageRepository consumedMessageRepository;

    /**
     * 构造消费归档服务。
     *
     * @param consumedMessageRepository 已消费消息仓储
     */
    public ConsumedMessageArchiveService(ConsumedMessageRepository consumedMessageRepository) {
        this.consumedMessageRepository = consumedMessageRepository;
    }

    /**
     * 按阈值归档消费记录。
     *
     * @param archiveBefore 归档阈值
     * @param limit 单次读取条数
     * @return 归档数量
     */
    public int archiveOlderThan(Date archiveBefore, int limit) {
        List<ConsumedMessageRecord> candidates = this.consumedMessageRepository.findArchiveCandidates(archiveBefore,
                limit);
        if (candidates.isEmpty()) {
            return 0;
        }
        return this.consumedMessageRepository.archive(candidates);
    }
}
