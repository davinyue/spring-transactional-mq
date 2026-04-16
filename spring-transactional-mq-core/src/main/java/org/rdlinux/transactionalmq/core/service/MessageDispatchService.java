package org.rdlinux.transactionalmq.core.service;

import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.SendStatus;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;
import org.rdlinux.transactionalmq.core.model.MessageSendLogRecord;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 事务消息派发服务骨架。
 */
public class MessageDispatchService {

    private static final int MAX_DESCRIPTION_LENGTH = 512;

    private final TransactionalMessageRepository transactionalMessageRepository;
    private final MqProducerAdapter mqProducerAdapter;
    private final MessageSendLogRepository messageSendLogRepository;

    /**
     * 构造消息派发服务。
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param mqProducerAdapter              MQ 适配器
     */
    public MessageDispatchService(TransactionalMessageRepository transactionalMessageRepository,
                                  MqProducerAdapter mqProducerAdapter) {
        this(transactionalMessageRepository, mqProducerAdapter, null);
    }

    /**
     * 构造消息派发服务。
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param mqProducerAdapter              MQ 适配器
     * @param messageSendLogRepository       发送日志仓储
     */
    public MessageDispatchService(TransactionalMessageRepository transactionalMessageRepository,
                                  MqProducerAdapter mqProducerAdapter, MessageSendLogRepository messageSendLogRepository) {
        this.transactionalMessageRepository = transactionalMessageRepository;
        this.mqProducerAdapter = mqProducerAdapter;
        this.messageSendLogRepository = messageSendLogRepository;
    }

    /**
     * 领取待派发消息并交给 MQ 适配器。
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
                this.mqProducerAdapter.send(DispatchMessage.from(record));
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
        logRecord.setRetryCount(0);
        logRecord.setLastSendTime(new Date());
        logRecord.setDescription(this.truncateDescription(description));
        this.messageSendLogRepository.save(logRecord);
    }

    private String truncateDescription(String description) {
        if (description == null || description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_DESCRIPTION_LENGTH);
    }
}
