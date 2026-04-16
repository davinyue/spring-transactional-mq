package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import java.util.Date;

import javax.persistence.Table;

import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.common.enums.SendStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

/**
 * 消息发送日志表实体。
 */
@Getter
@Setter
@Accessors(chain = true)
@FieldNameConstants
@Table(name = "TXN_MESSAGE_SEND_LOG")
public class MessageSendLogEntity extends BaseEntity<MessageSendLogEntity> {

    /**
     * 消息键。
     */
    private String messageKey;
    /**
     * 生产者编码。
     */
    private String producerCode;
    /**
     * MQ 类型。
     */
    private MqType mqType;
    /**
     * 发送状态。
     */
    private SendStatus sendStatus;
    /**
     * 重试次数。
     */
    private Integer retryCount;
    /**
     * 最后发送时间。
     */
    private Date lastSendTime;
    /**
     * 结果描述。
     */
    private String description;
}
