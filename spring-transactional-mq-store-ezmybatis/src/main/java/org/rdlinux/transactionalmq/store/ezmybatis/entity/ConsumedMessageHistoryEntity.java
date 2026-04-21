package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.rdlinux.ezmybatis.annotation.TypeHandler;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.ConsumeStatus;

import javax.persistence.Table;
import java.util.Date;

/**
 * 已消费消息历史表实体
 */
@Getter
@Setter
@Accessors(chain = true)
@FieldNameConstants
@Table(name = "TXN_CONSUMED_MESSAGE_HISTORY")
public class ConsumedMessageHistoryEntity extends BaseEntity<ConsumedMessageHistoryEntity> {

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
    @TypeHandler(EnumOrdinalTypeHandler.class)
    private ConsumeStatus consumeStatus;
    /**
     * 消费时间
     */
    private Date consumeTime;
    /**
     * 归档时间
     */
    private Date archiveTime;
}
