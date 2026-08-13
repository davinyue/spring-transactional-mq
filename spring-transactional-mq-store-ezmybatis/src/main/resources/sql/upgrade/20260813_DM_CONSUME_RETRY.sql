ALTER TABLE TXN_MESSAGE ADD (
    original_message_id VARCHAR2(24) NULL,
    retry_count NUMBER(10) DEFAULT 0 NOT NULL,
    consumer_code VARCHAR2(64) NULL,
    last_error VARCHAR2(1000) NULL
);

UPDATE TXN_MESSAGE
SET original_message_id = id
WHERE original_message_id IS NULL;

ALTER TABLE TXN_MESSAGE MODIFY original_message_id VARCHAR2(24) NOT NULL;
ALTER TABLE TXN_MESSAGE ADD CONSTRAINT UK_TXN_MESSAGE_ORIGINAL_RETRY
    UNIQUE (original_message_id, retry_count);

ALTER TABLE TXN_MESSAGE_HISTORY ADD (
    original_message_id VARCHAR2(24) NULL,
    retry_count NUMBER(10) DEFAULT 0 NOT NULL,
    consumer_code VARCHAR2(64) NULL,
    last_error VARCHAR2(1000) NULL
);

UPDATE TXN_MESSAGE_HISTORY
SET original_message_id = id
WHERE original_message_id IS NULL;

ALTER TABLE TXN_MESSAGE_HISTORY MODIFY original_message_id VARCHAR2(24) NOT NULL;

COMMENT ON COLUMN TXN_MESSAGE.original_message_id IS '原始消息id';
COMMENT ON COLUMN TXN_MESSAGE.retry_count IS '已执行的消费重试次数';
COMMENT ON COLUMN TXN_MESSAGE.consumer_code IS '消费者编码';
COMMENT ON COLUMN TXN_MESSAGE.last_error IS '最后消费失败信息';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.original_message_id IS '原始消息id';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.retry_count IS '已执行的消费重试次数';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.consumer_code IS '消费者编码';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.last_error IS '最后消费失败信息';

COMMIT;
