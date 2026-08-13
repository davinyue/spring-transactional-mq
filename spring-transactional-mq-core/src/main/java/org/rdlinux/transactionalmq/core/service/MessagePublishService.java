package org.rdlinux.transactionalmq.core.service;

import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.producer.TransactionalMessageSender;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.common.id.ObjectIdGenerator;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Date;

/**
 * 事务消息发布服务骨架
 */
public class MessagePublishService implements TransactionalMessageSender {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final TransactionalMessageRepository transactionalMessageRepository;
    private final MessagePayloadSerializer messagePayloadSerializer;
    private final MessageDispatchWakeupService messageDispatchWakeupService;
    private final MqProducerRouter mqProducerRouter;

    /**
     * 构造消息发布服务
     *
     * @param transactionalMessageRepository 事务消息仓储
     * @param messagePayloadSerializer       消息负载序列化器
     * @param messageDispatchWakeupService   派发线程唤醒器
     * @param mqProducerRouter               MQ 生产者路由器
     */
    public MessagePublishService(TransactionalMessageRepository transactionalMessageRepository,
                                 MessagePayloadSerializer messagePayloadSerializer,
                                 MessageDispatchWakeupService messageDispatchWakeupService,
                                 MqProducerRouter mqProducerRouter) {
        this.transactionalMessageRepository = transactionalMessageRepository;
        this.messagePayloadSerializer = messagePayloadSerializer;
        this.messageDispatchWakeupService = messageDispatchWakeupService;
        this.mqProducerRouter = mqProducerRouter;
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
    public <T> String send(MqType mqType, TransactionalMessage<T> message) {
        return this.doSave(mqType, message, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <T> String sendWithParent(MqType mqType, TransactionalMessage<T> message, ConsumeContext parentContext) {
        return this.doSave(mqType, message, parentContext);
    }

    /**
     * 在独立事务中保存下一轮消费重试消息
     *
     * @param mqType         MQ 类型
     * @param message        待重试消息
     * @param consumeContext 当前消费上下文
     * @param delay          下一轮重试间隔
     * @param failureMessage 本次失败信息
     * @param <T>            负载类型
     * @return 是否首次创建该轮重试；返回 false 表示该轮重试已经存在
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public <T> boolean scheduleConsumeRetry(MqType mqType, TransactionalMessage<T> message,
                                            ConsumeContext consumeContext, Duration delay,
                                            String failureMessage) {
        this.validateDelay(delay);
        TransactionalMessageRecord record = this.buildConsumeRetryRecord(mqType, message, consumeContext,
                failureMessage);
        record.setMessageStatus(MessageStatus.INIT);
        record.setNextDispatchTime(new Date(Math.addExact(System.currentTimeMillis(), delay.toMillis())));
        boolean saved = this.saveConsumeRetryIfAbsent(record);
        if (saved) {
            this.notifyDispatchAfterCommit();
        }
        return saved;
    }

    /**
     * 在独立事务中保存消费者停止重试的失败审计记录
     *
     * @param mqType         MQ 类型
     * @param message        消费失败消息
     * @param consumeContext 当前消费上下文
     * @param failureMessage 本次失败信息
     * @param <T>            负载类型
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public <T> void recordConsumeRetryStopped(MqType mqType, TransactionalMessage<T> message,
                                              ConsumeContext consumeContext, String failureMessage) {
        TransactionalMessageRecord record = this.buildConsumeRetryRecord(mqType, message, consumeContext,
                failureMessage);
        record.setMessageStatus(MessageStatus.DEAD);
        record.setNextDispatchTime(null);
        this.saveConsumeRetryIfAbsent(record);
    }

    private <T> String doSave(MqType mqType, TransactionalMessage<T> message, ConsumeContext parentContext) {
        this.validateMqType(mqType);
        String payloadText = this.messagePayloadSerializer.serialize(message.getPayload());
        TransactionalMessageRecord record = parentContext == null
                ? TransactionalMessageRecord.from(mqType, message, payloadText)
                : TransactionalMessageRecord.from(mqType, message, payloadText, parentContext);
        this.ensureIds(record);
        TransactionalMessageRecord saved = this.transactionalMessageRepository.save(record);
        this.notifyDispatchAfterCommit();
        return saved.getId();
    }

    private void validateMqType(MqType mqType) {
        if (mqType == null) {
            throw new IllegalArgumentException("mqType must not be null");
        }
        if (this.mqProducerRouter == null || !this.mqProducerRouter.supports(mqType)) {
            throw new IllegalArgumentException("unsupported mqType: " + mqType);
        }
    }

    private void ensureIds(TransactionalMessageRecord record) {
        if (record.getId() == null || record.getId().trim().isEmpty()) {
            record.setId(ObjectIdGenerator.generate());
        }
        if (record.getOriginalMessageId() == null || record.getOriginalMessageId().trim().isEmpty()) {
            record.setOriginalMessageId(record.getId());
        }
        if (record.getRetryCount() == null) {
            record.setRetryCount(0);
        }
        if (record.getRootId() == null || record.getRootId().trim().isEmpty()) {
            record.setRootId(record.getId());
        }
    }

    /**
     * 构建下一轮消费重试记录
     *
     * @param mqType         MQ 类型
     * @param message        消息
     * @param consumeContext 当前消费上下文
     * @param failureMessage 本次失败信息
     * @param <T>            负载类型
     * @return 下一轮消费重试记录
     */
    private <T> TransactionalMessageRecord buildConsumeRetryRecord(MqType mqType, TransactionalMessage<T> message,
                                                                   ConsumeContext consumeContext,
                                                                   String failureMessage) {
        this.validateMqType(mqType);
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (consumeContext == null || consumeContext.getId() == null
                || consumeContext.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("consumeContext message id must not be blank");
        }
        if (consumeContext.getRetryCount() < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        String payloadText = this.messagePayloadSerializer.serialize(message.getPayload());
        TransactionalMessageRecord record = TransactionalMessageRecord.from(mqType, message, payloadText);
        this.ensureIds(record);
        record.setOriginalMessageId(this.resolveOriginalMessageId(consumeContext));
        record.setRetryCount(Math.addExact(consumeContext.getRetryCount(), 1));
        record.setConsumerCode(consumeContext.getConsumerCode());
        record.setParentId(consumeContext.getParentId());
        record.setRootId(consumeContext.getRootId());
        record.setLastError(this.truncateError(failureMessage));
        return record;
    }

    /**
     * 解析原始消息 id
     *
     * @param consumeContext 消费上下文
     * @return 原始消息 id
     */
    private String resolveOriginalMessageId(ConsumeContext consumeContext) {
        if (consumeContext.getOriginalMessageId() == null
                || consumeContext.getOriginalMessageId().trim().isEmpty()) {
            return consumeContext.getId();
        }
        return consumeContext.getOriginalMessageId();
    }

    /**
     * 校验重试间隔
     *
     * @param delay 重试间隔
     */
    private void validateDelay(Duration delay) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            throw new IllegalArgumentException("delay must be greater than zero");
        }
    }

    /**
     * 截断失败信息
     *
     * @param failureMessage 失败信息
     * @return 可持久化的失败信息
     */
    private String truncateError(String failureMessage) {
        if (failureMessage == null || failureMessage.length() <= MAX_ERROR_LENGTH) {
            return failureMessage;
        }
        return failureMessage.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * 保存消费重试记录，并将唯一键冲突视为记录已存在。
     *
     * <p>仓储实现应让唯一键异常跨越独立事务边界后再由本方法捕获，避免部分数据库在语句失败后
     * 将当前事务保持为不可提交状态。</p>
     *
     * @param record 消费重试记录
     * @return 是否首次创建记录
     */
    private boolean saveConsumeRetryIfAbsent(TransactionalMessageRecord record) {
        try {
            this.transactionalMessageRepository.saveConsumeRetry(record);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
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
