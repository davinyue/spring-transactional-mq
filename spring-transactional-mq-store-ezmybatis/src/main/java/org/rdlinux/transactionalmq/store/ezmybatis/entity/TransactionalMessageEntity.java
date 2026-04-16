package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import java.util.Date;

import javax.persistence.Table;

import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

/**
 * 事务消息表实体。
 */
@Getter
@Setter
@Accessors(chain = true)
@FieldNameConstants
@Table(name = "TXN_MESSAGE")
public class TransactionalMessageEntity extends BaseEntity<TransactionalMessageEntity> {

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
     * 目标。
     */
    private String destination;
    /**
     * 路由信息。
     */
    private String route;
    /**
     * 分片键。
     */
    private String shardingKey;
    /**
     * 消息体文本。
     */
    private String payloadText;
    /**
     * 消息头 JSON。
     */
    private String headersJson;
    /**
     * 业务键。
     */
    private String bizKey;
    /**
     * 消息状态。
     */
    private MessageStatus messageStatus;
    /**
     * 下次派发时间。
     */
    private Date nextDispatchTime;
    /**
     * 派发实例标识。
     */
    private String dispatchOwner;
    /**
     * 派发令牌。
     */
    private String dispatchToken;
    /**
     * 派发租约过期时间。
     */
    private Date dispatchExpireTime;
}
