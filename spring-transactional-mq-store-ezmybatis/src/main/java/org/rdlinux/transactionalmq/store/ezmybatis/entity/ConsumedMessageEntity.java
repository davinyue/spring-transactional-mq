package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import java.util.Date;

import javax.persistence.Table;

import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.ConsumeStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

/**
 * 已消费消息表实体
 */
@Getter
@Setter
@Accessors(chain = true)
@FieldNameConstants
@Table(name = "TXN_CONSUMED_MESSAGE")
public class ConsumedMessageEntity extends BaseEntity<ConsumedMessageEntity> {

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
}
