package org.rdlinux.transactionalmq.core.service;

import java.util.Date;
import java.util.List;

import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;

/**
 * 已消费消息主表清理服务
 *
 * <p>消费记录在首次消费时已经同步写入历史表，因此这里的职责仅是按阈值删除
 * `TXN_CONSUMED_MESSAGE` 主表中的旧记录，避免在线去重表持续膨胀</p>
 */
public class ConsumedMessageCleanupService {

    private final ConsumedMessageRepository consumedMessageRepository;

    /**
     * 构造消费记录清理服务
     *
     * @param consumedMessageRepository 已消费消息仓储
     */
    public ConsumedMessageCleanupService(ConsumedMessageRepository consumedMessageRepository) {
        this.consumedMessageRepository = consumedMessageRepository;
    }

    /**
     * 按阈值清理消费主表记录
     *
     * @param cleanupBefore 清理阈值
     * @param limit         单次读取条数
     * @return 清理数量
     */
    public int cleanupOlderThan(Date cleanupBefore, int limit) {
        List<ConsumedMessageRecord> candidates = this.consumedMessageRepository.findArchiveCandidates(cleanupBefore,
                limit);
        if (candidates.isEmpty()) {
            return 0;
        }
        return this.consumedMessageRepository.archive(candidates);
    }
}
