package org.rdlinux.transactionalmq.core.model;

import java.util.Date;

import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.ConsumeStatus;

/**
 * core 层消费记录
 */
public class ConsumedMessageRecord extends BaseEntity<ConsumedMessageRecord> {

    /**
     * 消息键
     */
    private String messageKey;
    /**
     * 消费者编码
     */
    private String consumerCode;
    /**
     * 业务键
     */
    private String bizKey;
    /**
     * 消费状态
     */
    private ConsumeStatus consumeStatus;
    /**
     * 消费时间
     */
    private Date consumeTime;

    /**
     * 从消费上下文创建消费记录
     *
     * <p>当前骨架实现不推进消费状态机，成功处理后的归档链路会直接依赖 {@link ConsumeStatus#SUCCESS}
     * 语义，因此这里默认落为成功状态，便于后续归档查询直接命中</p>
     *
     * @param context 消费上下文
     * @return 消费记录
     */
    public static ConsumedMessageRecord from(ConsumeContext context) {
        ConsumedMessageRecord record = new ConsumedMessageRecord();
        if (context != null) {
            record.setId(context.getId());
            record.setMessageKey(context.getMessageKey());
            record.setConsumerCode(context.getConsumerCode());
        }
        record.setConsumeStatus(ConsumeStatus.SUCCESS);
        record.setConsumeTime(new Date());
        return record;
    }

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
     * 获取消费者编码
     *
     * @return 消费者编码
     */
    public String getConsumerCode() {
        return this.consumerCode;
    }

    /**
     * 设置消费者编码
     *
     * @param consumerCode 消费者编码
     */
    public void setConsumerCode(String consumerCode) {
        this.consumerCode = consumerCode;
    }

    /**
     * 获取业务键
     *
     * @return 业务键
     */
    public String getBizKey() {
        return this.bizKey;
    }

    /**
     * 设置业务键
     *
     * @param bizKey 业务键
     */
    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    /**
     * 获取消费状态
     *
     * @return 消费状态
     */
    public ConsumeStatus getConsumeStatus() {
        return this.consumeStatus;
    }

    /**
     * 设置消费状态
     *
     * @param consumeStatus 消费状态
     */
    public void setConsumeStatus(ConsumeStatus consumeStatus) {
        this.consumeStatus = consumeStatus;
    }

    /**
     * 获取消费时间
     *
     * @return 消费时间
     */
    public Date getConsumeTime() {
        return this.consumeTime;
    }

    /**
     * 设置消费时间
     *
     * @param consumeTime 消费时间
     */
    public void setConsumeTime(Date consumeTime) {
        this.consumeTime = consumeTime;
    }
}
