CREATE TABLE IF NOT EXISTS TXN_MESSAGE (
    id VARCHAR(24) NOT NULL COMMENT '主键',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    message_key VARCHAR(128) NOT NULL COMMENT '消息键',
    producer_code VARCHAR(64) NOT NULL COMMENT '生产者编码',
    mq_type VARCHAR(32) NOT NULL COMMENT 'MQ 类型',
    destination VARCHAR(256) NOT NULL COMMENT '目标',
    route VARCHAR(256) NULL COMMENT '路由信息',
    sharding_key VARCHAR(128) NULL COMMENT '分片键',
    payload_text LONGTEXT NOT NULL COMMENT '消息体文本',
    headers_json LONGTEXT NOT NULL COMMENT '消息头 JSON',
    biz_key VARCHAR(128) NULL COMMENT '业务键',
    message_status VARCHAR(32) NOT NULL COMMENT '消息状态',
    next_dispatch_time DATETIME NULL COMMENT '下次派发时间',
    dispatch_owner VARCHAR(128) NULL COMMENT '派发实例标识',
    dispatch_token VARCHAR(24) NULL COMMENT '派发令牌',
    dispatch_expire_time DATETIME NULL COMMENT '派发租约过期时间',
    PRIMARY KEY (id),
    KEY idx_txn_message_status_dispatch (message_status, next_dispatch_time, id),
    KEY idx_txn_message_status_update (message_status, update_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务消息表';

CREATE TABLE IF NOT EXISTS TXN_MESSAGE_HISTORY (
    id VARCHAR(24) NOT NULL COMMENT '主键',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    message_key VARCHAR(128) NOT NULL COMMENT '消息键',
    producer_code VARCHAR(64) NOT NULL COMMENT '生产者编码',
    mq_type VARCHAR(32) NOT NULL COMMENT 'MQ 类型',
    destination VARCHAR(256) NOT NULL COMMENT '目标',
    route VARCHAR(256) NULL COMMENT '路由信息',
    sharding_key VARCHAR(128) NULL COMMENT '分片键',
    payload_text LONGTEXT NOT NULL COMMENT '消息体文本',
    headers_json LONGTEXT NOT NULL COMMENT '消息头 JSON',
    biz_key VARCHAR(128) NULL COMMENT '业务键',
    message_status VARCHAR(32) NOT NULL COMMENT '消息状态',
    next_dispatch_time DATETIME NULL COMMENT '下次派发时间',
    dispatch_owner VARCHAR(128) NULL COMMENT '派发实例标识',
    dispatch_token VARCHAR(24) NULL COMMENT '派发令牌',
    dispatch_expire_time DATETIME NULL COMMENT '派发租约过期时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务消息历史表';

CREATE TABLE IF NOT EXISTS TXN_CONSUMED_MESSAGE (
    id VARCHAR(24) NOT NULL COMMENT '主键',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    message_key VARCHAR(128) NOT NULL COMMENT '消息键',
    consumer_code VARCHAR(64) NOT NULL COMMENT '消费者编码',
    biz_key VARCHAR(128) NULL COMMENT '业务键',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态',
    consume_time DATETIME NOT NULL COMMENT '消费时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_txn_consumed_message (id, consumer_code),
    KEY idx_txn_consumed_message_time (consume_status, consume_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已消费消息主表';

CREATE TABLE IF NOT EXISTS TXN_CONSUMED_MESSAGE_HISTORY (
    id VARCHAR(24) NOT NULL COMMENT '主键',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    message_key VARCHAR(128) NOT NULL COMMENT '消息键',
    consumer_code VARCHAR(64) NOT NULL COMMENT '消费者编码',
    biz_key VARCHAR(128) NULL COMMENT '业务键',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态',
    consume_time DATETIME NOT NULL COMMENT '消费时间',
    archive_time DATETIME NOT NULL COMMENT '归档时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_txn_consumed_message_history (id, consumer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已消费消息历史表';

CREATE TABLE IF NOT EXISTS TXN_MESSAGE_SEND_LOG (
    id VARCHAR(24) NOT NULL COMMENT '主键',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    message_key VARCHAR(128) NOT NULL COMMENT '消息键',
    producer_code VARCHAR(64) NOT NULL COMMENT '生产者编码',
    mq_type VARCHAR(32) NOT NULL COMMENT 'MQ 类型',
    send_status VARCHAR(32) NOT NULL COMMENT '发送状态',
    retry_count INT NOT NULL COMMENT '重试次数',
    last_send_time DATETIME NOT NULL COMMENT '最后发送时间',
    description VARCHAR(512) NULL COMMENT '结果描述',
    PRIMARY KEY (id),
    KEY idx_txn_message_send_log_status (send_status, last_send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送日志表';
