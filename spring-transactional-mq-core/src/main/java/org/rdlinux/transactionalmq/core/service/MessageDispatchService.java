package org.rdlinux.transactionalmq.core.service;

import lombok.extern.slf4j.Slf4j;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.SendStatus;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;
import org.rdlinux.transactionalmq.core.model.MessageSendLogRecord;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 事务消息派发服务骨架
 */
@Slf4j
public class MessageDispatchService {

    /**
     * 发送描述最大长度
     */
    private static final int MAX_DESCRIPTION_LENGTH = 512;

    /**
     * 事务消息仓储
     */
    private final TransactionalMessageRepository transactionalMessageRepository;
    /**
     * MQ 生产者路由器
     */
    private final MqProducerRouter mqProducerRouter;
    /**
     * 发送日志仓储
     */
    private final MessageSendLogRepository messageSendLogRepository;

    /**
     * 构造消息派发服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param mqProducerRouter               MQ 路由器
     */
    public MessageDispatchService(TransactionalMessageRepository transactionalMessageRepository,
                                  MqProducerRouter mqProducerRouter) {
        this(transactionalMessageRepository, mqProducerRouter, null);
    }

    /**
     * 构造消息派发服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param mqProducerRouter               MQ 路由器
     * @param messageSendLogRepository       发送日志仓储
     */
    public MessageDispatchService(TransactionalMessageRepository transactionalMessageRepository,
                                  MqProducerRouter mqProducerRouter, MessageSendLogRepository messageSendLogRepository) {
        this.transactionalMessageRepository = transactionalMessageRepository;
        this.mqProducerRouter = mqProducerRouter;
        this.messageSendLogRepository = messageSendLogRepository;
    }

    /**
     * 领取待派发消息并交给 MQ 适配器
     *
     * @param limit 领取条数
     * @return 本次派发数量
     */
    public int dispatchPendingMessages(int limit) {
        List<TransactionalMessageRecord> candidates = this.transactionalMessageRepository.findDispatchCandidates(limit);
        candidates.sort(Comparator.comparing(BaseEntity::getId));
        List<TransactionalMessageRecord> successRecords = new ArrayList<>();
        List<TransactionalMessageRecord> failedRecords = new ArrayList<>();
        for (TransactionalMessageRecord candidate : candidates) {
            TransactionalMessageRecord record = this.transactionalMessageRepository.claimDispatchMessage(candidate);
            if (record == null) {
                continue;
            }
            try {
                this.mqProducerRouter.send(DispatchMessage.from(record));
            } catch (RuntimeException ex) {
                this.saveSendLog(record, SendStatus.FAILED, ex.getMessage());
                failedRecords.add(record);
                continue;
            }
            this.saveSendLog(record, SendStatus.SUCCESS, "send success");
            successRecords.add(record);
        }
        this.transactionalMessageRepository.markDispatchSuccess(successRecords);
        this.transactionalMessageRepository.markDispatchFailed(failedRecords);
        return successRecords.size() + failedRecords.size();
    }

    /**
     * 保存消息发送日志
     *
     * @param record 消息记录
     * @param sendStatus 发送状态
     * @param description 发送描述
     */
    private void saveSendLog(TransactionalMessageRecord record, SendStatus sendStatus, String description) {
        if (this.messageSendLogRepository == null) {
            return;
        }
        MessageSendLogRecord logRecord = new MessageSendLogRecord();
        logRecord.setId(record.getId());
        logRecord.setMessageKey(record.getMessageKey());
        logRecord.setProducerCode(record.getProducerCode());
        logRecord.setMqType(record.getMqType());
        logRecord.setParentId(record.getParentId());
        logRecord.setRootId(record.getRootId());
        logRecord.setSendStatus(sendStatus);
        logRecord.setRetryCount(record.getRetryCount() == null ? 0 : record.getRetryCount());
        logRecord.setLastSendTime(new Date());
        logRecord.setDescription(this.truncateDescription(description));
        try {
            this.messageSendLogRepository.save(logRecord);
        } catch (DuplicateKeyException ex) {
            log.debug("Ignore duplicate message send log, messageId={}", record.getId());
        }
    }

    /**
     * 截断发送描述
     *
     * @param description 原始发送描述
     * @return 截断后的发送描述
     */
    private String truncateDescription(String description) {
        if (description == null || description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_DESCRIPTION_LENGTH);
    }
}
