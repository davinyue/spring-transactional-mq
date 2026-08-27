ALTER TABLE TXN_MESSAGE
    ADD COLUMN original_message_id VARCHAR(24) NULL,
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN consumer_code VARCHAR(64) NULL,
    ADD COLUMN last_error VARCHAR(1000) NULL;

UPDATE TXN_MESSAGE
SET original_message_id = id
WHERE original_message_id IS NULL;

ALTER TABLE TXN_MESSAGE ALTER COLUMN original_message_id SET NOT NULL;
ALTER TABLE TXN_MESSAGE ALTER COLUMN retry_count DROP DEFAULT;
CREATE UNIQUE INDEX uk_txn_message_original_retry
    ON TXN_MESSAGE (original_message_id, retry_count);

ALTER TABLE TXN_MESSAGE_HISTORY
    ADD COLUMN original_message_id VARCHAR(24) NULL,
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN consumer_code VARCHAR(64) NULL,
    ADD COLUMN last_error VARCHAR(1000) NULL;

UPDATE TXN_MESSAGE_HISTORY
SET original_message_id = id
WHERE original_message_id IS NULL;

ALTER TABLE TXN_MESSAGE_HISTORY ALTER COLUMN original_message_id SET NOT NULL;
ALTER TABLE TXN_MESSAGE_HISTORY ALTER COLUMN retry_count DROP DEFAULT;
CREATE UNIQUE INDEX uk_txn_message_history_original_retry
    ON TXN_MESSAGE_HISTORY (original_message_id, retry_count);

COMMENT ON COLUMN TXN_MESSAGE.original_message_id IS '原始消息id';
COMMENT ON COLUMN TXN_MESSAGE.retry_count IS '已执行的消费重试次数';
COMMENT ON COLUMN TXN_MESSAGE.consumer_code IS '消费者编码';
COMMENT ON COLUMN TXN_MESSAGE.last_error IS '最后消费失败信息';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.original_message_id IS '原始消息id';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.retry_count IS '已执行的消费重试次数';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.consumer_code IS '消费者编码';
COMMENT ON COLUMN TXN_MESSAGE_HISTORY.last_error IS '最后消费失败信息';
