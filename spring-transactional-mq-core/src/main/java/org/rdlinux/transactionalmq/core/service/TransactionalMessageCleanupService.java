package org.rdlinux.transactionalmq.core.service;

import java.util.Date;
import java.util.Comparator;
import java.util.List;

import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;

/**
 * 事务消息主表归档清理服务
 */
public class TransactionalMessageCleanupService {

    private final TransactionalMessageRepository transactionalMessageRepository;

    /**
     * 构造事务消息主表清理服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     */
    public TransactionalMessageCleanupService(TransactionalMessageRepository transactionalMessageRepository) {
        this.transactionalMessageRepository = transactionalMessageRepository;
    }

    /**
     * 归档并清理已发送成功的事务消息主表记录
     *
     * @param cleanupBefore 清理阈值时间
     * @param limit         最大清理条数
     * @return 归档并清理条数
     */
    public int cleanupSuccessMessages(Date cleanupBefore, int limit) {
        List<TransactionalMessageRecord> candidates =
                this.transactionalMessageRepository.findSuccessCleanupCandidates(cleanupBefore, limit);
        candidates.sort(Comparator.comparing(TransactionalMessageRecord::getId));
        int archived = 0;
        for (TransactionalMessageRecord candidate : candidates) {
            archived += this.transactionalMessageRepository.archiveSuccessMessage(candidate, cleanupBefore);
        }
        return archived;
    }
}
