CREATE TABLE TXN_MESSAGE (
    id NVARCHAR(24) NOT NULL PRIMARY KEY,
    create_time DATETIME2 NOT NULL,
    update_time DATETIME2 NOT NULL,
    message_key NVARCHAR(128) NOT NULL,
    producer_code NVARCHAR(64) NOT NULL,
    mq_type INT NOT NULL,
    destination NVARCHAR(256) NOT NULL,
    route NVARCHAR(256) NULL,
    sharding_key NVARCHAR(128) NULL,
    payload_text NVARCHAR(MAX) NOT NULL,
    headers_json NVARCHAR(MAX) NOT NULL,
    biz_key NVARCHAR(128) NULL,
    message_status INT NOT NULL,
    next_dispatch_time DATETIME2 NULL,
    parent_id NVARCHAR(24) NULL,
    root_id NVARCHAR(24) NULL,
    dispatch_owner NVARCHAR(128) NULL,
    dispatch_token NVARCHAR(24) NULL,
    dispatch_expire_time DATETIME2 NULL
);
GO
CREATE INDEX IDX_TXN_MESSAGE_STATUS_DISPATCH ON TXN_MESSAGE(message_status, next_dispatch_time, id);
GO
CREATE INDEX IDX_TXN_MESSAGE_STATUS_UPDATE ON TXN_MESSAGE(message_status, update_time, id);
GO

CREATE TABLE TXN_MESSAGE_HISTORY (
    id NVARCHAR(24) NOT NULL PRIMARY KEY,
    create_time DATETIME2 NOT NULL,
    update_time DATETIME2 NOT NULL,
    message_key NVARCHAR(128) NOT NULL,
    producer_code NVARCHAR(64) NOT NULL,
    mq_type INT NOT NULL,
    destination NVARCHAR(256) NOT NULL,
    route NVARCHAR(256) NULL,
    sharding_key NVARCHAR(128) NULL,
    payload_text NVARCHAR(MAX) NOT NULL,
    headers_json NVARCHAR(MAX) NOT NULL,
    biz_key NVARCHAR(128) NULL,
    message_status INT NOT NULL,
    next_dispatch_time DATETIME2 NULL,
    parent_id NVARCHAR(24) NULL,
    root_id NVARCHAR(24) NULL,
    dispatch_owner NVARCHAR(128) NULL,
    dispatch_token NVARCHAR(24) NULL,
    dispatch_expire_time DATETIME2 NULL
);
GO

CREATE TABLE TXN_CONSUMED_MESSAGE (
    id NVARCHAR(24) NOT NULL PRIMARY KEY,
    create_time DATETIME2 NOT NULL,
    update_time DATETIME2 NOT NULL,
    message_key NVARCHAR(128) NOT NULL,
    consumer_code NVARCHAR(64) NOT NULL,
    biz_key NVARCHAR(128) NULL,
    consume_status INT NOT NULL,
    consume_time DATETIME2 NOT NULL
);
GO
CREATE UNIQUE INDEX UK_TXN_CONSUMED_MESSAGE ON TXN_CONSUMED_MESSAGE(id, consumer_code);
GO

CREATE TABLE TXN_CONSUMED_MESSAGE_HISTORY (
    id NVARCHAR(24) NOT NULL PRIMARY KEY,
    create_time DATETIME2 NOT NULL,
    update_time DATETIME2 NOT NULL,
    message_key NVARCHAR(128) NOT NULL,
    consumer_code NVARCHAR(64) NOT NULL,
    biz_key NVARCHAR(128) NULL,
    consume_status INT NOT NULL,
    consume_time DATETIME2 NOT NULL,
    archive_time DATETIME2 NOT NULL
);
GO
CREATE UNIQUE INDEX UK_TXN_CONSUMED_MESSAGE_HISTORY ON TXN_CONSUMED_MESSAGE_HISTORY(id, consumer_code);
GO

CREATE TABLE TXN_MESSAGE_SEND_LOG (
    id NVARCHAR(24) NOT NULL PRIMARY KEY,
    create_time DATETIME2 NOT NULL,
    update_time DATETIME2 NOT NULL,
    message_key NVARCHAR(128) NOT NULL,
    parent_id NVARCHAR(24) NULL,
    root_id NVARCHAR(24) NULL,
    producer_code NVARCHAR(64) NOT NULL,
    mq_type INT NOT NULL,
    send_status INT NOT NULL,
    retry_count INT NOT NULL,
    last_send_time DATETIME2 NOT NULL,
    description NVARCHAR(512) NULL
);
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'事务消息表', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'主键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'create_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'update_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'message_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'生产者编码', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'producer_code';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'MQ 类型', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'mq_type';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'目标', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'destination';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'路由信息', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'route';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'分片键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'sharding_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息体文本', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'payload_text';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息头 JSON', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'headers_json';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'业务键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'biz_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息状态', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'message_status';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'下次派发时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'next_dispatch_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'父消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'parent_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'根消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'root_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'派发实例标识', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'dispatch_owner';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'派发令牌', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'dispatch_token';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'派发租约过期时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'dispatch_expire_time';
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'事务消息历史表', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'主键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'create_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'update_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'message_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'生产者编码', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'producer_code';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'MQ 类型', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'mq_type';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'目标', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'destination';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'路由信息', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'route';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'分片键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'sharding_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息体文本', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'payload_text';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息头 JSON', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'headers_json';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'业务键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'biz_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息状态', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'message_status';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'下次派发时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'next_dispatch_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'父消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'parent_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'根消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'root_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'派发实例标识', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'dispatch_owner';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'派发令牌', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'dispatch_token';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'派发租约过期时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'dispatch_expire_time';
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'已消费消息主表', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'已消费消息历史表', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息发送日志表', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG';
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'主键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'create_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'update_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'message_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费者编码', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'consumer_code';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'业务键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'biz_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费状态', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'consume_status';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE', @level2type=N'COLUMN', @level2name=N'consume_time';
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'主键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'create_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'update_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'message_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费者编码', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'consumer_code';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'业务键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'biz_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费状态', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'consume_status';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'consume_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'归档时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_CONSUMED_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'archive_time';
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'主键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'create_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'update_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消息键', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'message_key';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'父消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'parent_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'根消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'root_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'生产者编码', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'producer_code';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'MQ 类型', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'mq_type';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'发送状态', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'send_status';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'重试次数', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'retry_count';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'最后发送时间', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'last_send_time';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'结果描述', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_SEND_LOG', @level2type=N'COLUMN', @level2name=N'description';
GO
