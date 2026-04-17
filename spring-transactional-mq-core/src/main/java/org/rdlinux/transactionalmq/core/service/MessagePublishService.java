package org.rdlinux.transactionalmq.core.service;

import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.producer.TransactionalMessageSender;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.id.ObjectIdGenerator;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务消息发布服务骨架
 */
public class MessagePublishService implements TransactionalMessageSender {

    private final TransactionalMessageRepository transactionalMessageRepository;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final MessageDispatchWakeupService messageDispatchWakeupService;

    /**
     * 构造消息发布服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param messagePayloadSerializer       消息负载序列化器
     * @param messageDispatchWakeupService   派发线程唤醒器
     */
    public MessagePublishService(TransactionalMessageRepository transactionalMessageRepository,
                                 MessagePayloadSerializer messagePayloadSerializer,
                                 MessageDispatchWakeupService messageDispatchWakeupService) {
        this.transactionalMessageRepository = transactionalMessageRepository;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.messageDispatchWakeupService = messageDispatchWakeupService;
    }

    /**
     * 保存消息并返回消息 id
     *
     * @param message 事务消息
     * @param <T>     负载类型
     * @return 消息 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public <T> String send(TransactionalMessage<T> message) {
        return this.doSave(message, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <T> String sendWithParent(TransactionalMessage<T> message, ConsumeContext parentContext) {
        return this.doSave(message, parentContext);
    }

    private <T> String doSave(TransactionalMessage<T> message, ConsumeContext parentContext) {
        String payloadText = this.messagePayloadSerializer.serialize(message.getPayload());
        TransactionalMessageRecord record = parentContext == null
                ? TransactionalMessageRecord.from(message, payloadText)
                : TransactionalMessageRecord.from(message, payloadText, parentContext);
        this.ensureIds(record);
        TransactionalMessageRecord saved = this.transactionalMessageRepository.save(record);
        this.notifyDispatchAfterCommit();
        return saved.getId();
    }

    private void ensureIds(TransactionalMessageRecord record) {
        if (record.getId() == null || record.getId().trim().isEmpty()) {
            record.setId(ObjectIdGenerator.generate());
        }
        if (record.getRootId() == null || record.getRootId().trim().isEmpty()) {
            record.setRootId(record.getId());
        }
    }

    private void notifyDispatchAfterCommit() {
        if (this.messageDispatchWakeupService == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    MessagePublishService.this.messageDispatchWakeupService.wakeup();
                }
            });
            return;
        }
        this.messageDispatchWakeupService.wakeup();
    }
}
