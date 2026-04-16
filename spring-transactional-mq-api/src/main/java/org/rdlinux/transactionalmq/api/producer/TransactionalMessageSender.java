package org.rdlinux.transactionalmq.api.producer;

import org.rdlinux.transactionalmq.api.model.SendResult;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;

/**
 * 事务消息发送器。
 */
public interface TransactionalMessageSender {

    /**
     * 发送事务消息。
     *
     * @param message 事务消息
     * @param <T> 消息负载类型
     * @return 发送结果
     */
    <T> SendResult send(TransactionalMessage<T> message);
}
