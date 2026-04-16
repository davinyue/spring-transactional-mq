package org.rdlinux.transactionalmq.api.model;

import java.util.HashMap;
import java.util.Map;

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
     * 父消息 id。
     */
    private String parentId;
    /**
     * 根消息 id。
     */
    private String rootId;
    /**
     * 原始消息头。
     */
    private Map<String, String> headers = new HashMap<String, String>();
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
     * 获取父消息 id。
     *
     * @return 父消息 id
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * 设置父消息 id。
     *
     * @param parentId 父消息 id
     * @return 当前上下文对象
     */
    public ConsumeContext setParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    /**
     * 获取根消息 id。
     *
     * @return 根消息 id
     */
    public String getRootId() {
        return rootId;
    }

    /**
     * 设置根消息 id。
     *
     * @param rootId 根消息 id
     * @return 当前上下文对象
     */
    public ConsumeContext setRootId(String rootId) {
        this.rootId = rootId;
        return this;
    }

    /**
     * 获取原始消息头。
     *
     * @return 原始消息头
     */
    public Map<String, String> getHeaders() {
        return new HashMap<String, String>(headers);
    }

    /**
     * 设置原始消息头。
     *
     * @param headers 原始消息头
     * @return 当前上下文对象
     */
    public ConsumeContext setHeaders(Map<String, String> headers) {
        if (headers == null) {
            this.headers = new HashMap<String, String>();
        } else {
            this.headers = new HashMap<String, String>(headers);
        }
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
