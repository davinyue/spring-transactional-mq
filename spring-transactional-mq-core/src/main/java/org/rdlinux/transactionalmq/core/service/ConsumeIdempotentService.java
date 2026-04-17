package org.rdlinux.transactionalmq.core.service;

import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.core.model.ConsumedMessageRecord;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;

/**
 * 消费幂等服务骨架
 */
public class ConsumeIdempotentService {

    private final ConsumedMessageRepository consumedMessageRepository;

    /**
     * 构造消费幂等服务
     *
     * @param consumedMessageRepository 已消费消息仓储
     */
    public ConsumeIdempotentService(ConsumedMessageRepository consumedMessageRepository) {
        this.consumedMessageRepository = consumedMessageRepository;
    }

    /**
     * 原子保存消费记录，首次保存成功才返回 true
     *
     * @param context 消费上下文
     * @return 是否成功记录
     */
    public boolean recordIfAbsent(ConsumeContext context) {
        return this.consumedMessageRepository.saveIfAbsent(ConsumedMessageRecord.from(context));
    }
}
