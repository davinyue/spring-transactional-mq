package org.rdlinux.transactionalmq.core.model;

import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.common.enums.SendStatus;

import java.util.Date;

/**
 * core 层发送日志
 */
public class MessageSendLogRecord extends BaseEntity<MessageSendLogRecord> {

    /**
     * 消息键
     */
    private String messageKey;
    /**
     * 生产者编码
     */
    private String producerCode;
    /**
     * MQ 类型
     */
    private MqType mqType;
    /**
     * 发送状态
     */
    private SendStatus sendStatus;
    /**
     * 重试次数
     */
    private Integer retryCount;
    /**
     * 最后发送时间
     */
    private Date lastSendTime;
    /**
     * 发送结果描述
     */
    private String description;
    /**
     * 父消息 id
     */
    private String parentId;
    /**
     * 根消息 id
     */
    private String rootId;

    /**
     * 获取消息键
     *
     * @return 消息键
     */
    public String getMessageKey() {
        return this.messageKey;
    }

    /**
     * 设置消息键
     *
     * @param messageKey 消息键
     */
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * 获取生产者编码
     *
     * @return 生产者编码
     */
    public String getProducerCode() {
        return this.producerCode;
    }

    /**
     * 设置生产者编码
     *
     * @param producerCode 生产者编码
     */
    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    /**
     * 获取 MQ 类型
     *
     * @return MQ 类型
     */
    public MqType getMqType() {
        return this.mqType;
    }

    /**
     * 设置 MQ 类型
     *
     * @param mqType MQ 类型
     */
    public void setMqType(MqType mqType) {
        this.mqType = mqType;
    }

    /**
     * 获取发送状态
     *
     * @return 发送状态
     */
    public SendStatus getSendStatus() {
        return this.sendStatus;
    }

    /**
     * 设置发送状态
     *
     * @param sendStatus 发送状态
     */
    public void setSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    /**
     * 获取重试次数
     *
     * @return 重试次数
     */
    public Integer getRetryCount() {
        return this.retryCount;
    }

    /**
     * 设置重试次数
     *
     * @param retryCount 重试次数
     */
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * 获取最后发送时间
     *
     * @return 最后发送时间
     */
    public Date getLastSendTime() {
        return this.lastSendTime;
    }

    /**
     * 设置最后发送时间
     *
     * @param lastSendTime 最后发送时间
     */
    public void setLastSendTime(Date lastSendTime) {
        this.lastSendTime = lastSendTime;
    }

    /**
     * 获取发送结果描述
     *
     * @return 发送结果描述
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 设置发送结果描述
     *
     * @param description 发送结果描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取父消息 id
     *
     * @return 父消息 id
     */
    public String getParentId() {
        return this.parentId;
    }

    /**
     * 设置父消息 id
     *
     * @param parentId 父消息 id
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * 获取根消息 id
     *
     * @return 根消息 id
     */
    public String getRootId() {
        return this.rootId;
    }

    /**
     * 设置根消息 id
     *
     * @param rootId 根消息 id
     */
    public void setRootId(String rootId) {
        this.rootId = rootId;
    }
}
