package org.rdlinux.transactionalmq.store.ezmybatis.entity;

import javax.persistence.Table;

/**
 * 事务消息历史表实体
 */
@Table(name = "TXN_MESSAGE_HISTORY")
public class TransactionalMessageHistoryEntity extends TransactionalMessageEntity {
}
