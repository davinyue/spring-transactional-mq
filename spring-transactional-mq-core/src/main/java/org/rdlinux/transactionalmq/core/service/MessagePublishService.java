package org.rdlinux.transactionalmq.core.service;

import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.producer.TransactionalMessageSender;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.rdlinux.transactionalmq.common.id.ObjectIdGenerator;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务消息发布服务骨架。
 */
public class MessagePublishService implements TransactionalMessageSender {

    private final TransactionalMessageRepository transactionalMessageRepository;
    private final MessagePayloadSerializer messagePayloadSerializer;

    /**
     * 构造消息发布服务。
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param messagePayloadSerializer 消息负载序列化器
     */
    public MessagePublishService(TransactionalMessageRepository transactionalMessageRepository,
            MessagePayloadSerializer messagePayloadSerializer) {
        this.transactionalMessageRepository = transactionalMessageRepository;
        this.messagePayloadSerializer = messagePayloadSerializer;
    }

    /**
     * 保存消息并返回消息 id。
     *
     * @param message 事务消息
     * @param <T> 负载类型
     * @return 消息 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public <T> String send(TransactionalMessage<T> message) {
        return doSave(message, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <T> String sendWithParent(TransactionalMessage<T> message, ConsumeContext parentContext) {
        return doSave(message, parentContext);
    }

    private <T> String doSave(TransactionalMessage<T> message, ConsumeContext parentContext) {
        String payloadText = this.messagePayloadSerializer.serialize(message.getPayload());
        TransactionalMessageRecord record = parentContext == null
            ? TransactionalMessageRecord.from(message, payloadText)
            : TransactionalMessageRecord.from(message, payloadText, parentContext);
        ensureIds(record);
        TransactionalMessageRecord saved = this.transactionalMessageRepository.save(record);
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
}
