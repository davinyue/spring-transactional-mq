ALTER TABLE TXN_MESSAGE ADD
    original_message_id NVARCHAR(24) NULL,
    retry_count INT NOT NULL CONSTRAINT DF_TXN_MESSAGE_RETRY_COUNT DEFAULT 0,
    consumer_code NVARCHAR(64) NULL,
    last_error NVARCHAR(1000) NULL;
GO

UPDATE TXN_MESSAGE
SET original_message_id = id
WHERE original_message_id IS NULL;
GO

ALTER TABLE TXN_MESSAGE ALTER COLUMN original_message_id NVARCHAR(24) NOT NULL;
GO
ALTER TABLE TXN_MESSAGE DROP CONSTRAINT DF_TXN_MESSAGE_RETRY_COUNT;
GO
CREATE UNIQUE INDEX UK_TXN_MESSAGE_ORIGINAL_RETRY
    ON TXN_MESSAGE(original_message_id, retry_count);
GO

ALTER TABLE TXN_MESSAGE_HISTORY ADD
    original_message_id NVARCHAR(24) NULL,
    retry_count INT NOT NULL CONSTRAINT DF_TXN_MESSAGE_HISTORY_RETRY_COUNT DEFAULT 0,
    consumer_code NVARCHAR(64) NULL,
    last_error NVARCHAR(1000) NULL;
GO

UPDATE TXN_MESSAGE_HISTORY
SET original_message_id = id
WHERE original_message_id IS NULL;
GO

ALTER TABLE TXN_MESSAGE_HISTORY ALTER COLUMN original_message_id NVARCHAR(24) NOT NULL;
GO
ALTER TABLE TXN_MESSAGE_HISTORY DROP CONSTRAINT DF_TXN_MESSAGE_HISTORY_RETRY_COUNT;
GO

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'原始消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'original_message_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'已执行的消费重试次数', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'retry_count';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费者编码', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'consumer_code';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'最后消费失败信息', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE', @level2type=N'COLUMN', @level2name=N'last_error';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'原始消息id', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'original_message_id';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'已执行的消费重试次数', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'retry_count';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'消费者编码', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'consumer_code';
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'最后消费失败信息', @level0type=N'SCHEMA',
    @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'TXN_MESSAGE_HISTORY', @level2type=N'COLUMN', @level2name=N'last_error';
GO
