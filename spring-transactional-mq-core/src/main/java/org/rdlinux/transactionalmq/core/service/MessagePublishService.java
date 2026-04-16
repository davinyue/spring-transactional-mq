package org.rdlinux.transactionalmq.core.service;

import org.rdlinux.transactionalmq.api.model.SendResult;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;

/**
 * 事务消息发布服务骨架。
 */
public class MessagePublishService {

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
     * 保存消息并返回已接收结果。
     *
     * @param message 事务消息
     * @param <T> 负载类型
     * @return 接收结果
     */
    public <T> SendResult publish(TransactionalMessage<T> message) {
        String payloadText = this.messagePayloadSerializer.serialize(message.getPayload());
        TransactionalMessageRecord record = TransactionalMessageRecord.from(message, payloadText);
        TransactionalMessageRecord saved = this.transactionalMessageRepository.save(record);
        return SendResult.accepted(saved.getId(), saved.getMessageKey());
    }
}
