package org.rdlinux.transactionalmq.api.model;

/**
 * 消费上下文。
 */
public class ConsumeContext {

    /**
     * 消息主键。
     */
    private String id;
    /**
     * 消息键。
     */
    private String messageKey;
    /**
     * 消费者编码。
     */
    private String consumerCode;

    /**
     * 获取消息主键。
     *
     * @return 消息主键
     */
    public String getId() {
        return id;
    }

    /**
     * 设置消息主键。
     *
     * @param id 消息主键
     * @return 当前上下文对象
     */
    public ConsumeContext setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * 获取消息键。
     *
     * @return 消息键
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * 设置消息键。
     *
     * @param messageKey 消息键
     * @return 当前上下文对象
     */
    public ConsumeContext setMessageKey(String messageKey) {
        this.messageKey = messageKey;
        return this;
    }

    /**
     * 获取消费者编码。
     *
     * @return 消费者编码
     */
    public String getConsumerCode() {
        return consumerCode;
    }

    /**
     * 设置消费者编码。
     *
     * @param consumerCode 消费者编码
     * @return 当前上下文对象
     */
    public ConsumeContext setConsumerCode(String consumerCode) {
        this.consumerCode = consumerCode;
        return this;
    }
}
