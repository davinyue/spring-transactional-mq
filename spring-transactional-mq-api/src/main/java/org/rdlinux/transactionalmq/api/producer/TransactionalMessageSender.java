package org.rdlinux.transactionalmq.api.producer;

import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;

/**
 * 事务消息发送器
 */
public interface TransactionalMessageSender {

    /**
     * 发送事务消息
     *
     * @param message 事务消息
     * @param <T>     消息负载类型
     * @return 消息 id
     */
    <T> String send(TransactionalMessage<T> message);

    /**
     * 基于父消息链路信息发送新的事务消息
     *
     * <p>该方法只续接链路字段，不会复制父消息的业务内容调用方应自行构造新的
     * {@link TransactionalMessage}，框架会根据父上下文自动补齐 {@code parentId} 和
     * {@code rootId}</p>
     *
     * @param message       新的事务消息
     * @param parentContext 父消息消费上下文
     * @param <T>           消息负载类型
     * @return 新消息 id
     */
    <T> String sendWithParent(TransactionalMessage<T> message, ConsumeContext parentContext);
}
