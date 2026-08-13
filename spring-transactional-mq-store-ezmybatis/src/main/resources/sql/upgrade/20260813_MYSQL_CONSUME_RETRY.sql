ALTER TABLE TXN_MESSAGE
    ADD COLUMN original_message_id VARCHAR(24) NULL COMMENT '原始消息id' AFTER next_dispatch_time,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '已执行的消费重试次数' AFTER original_message_id,
    ADD COLUMN consumer_code VARCHAR(64) NULL COMMENT '消费者编码' AFTER retry_count,
    ADD COLUMN last_error VARCHAR(1000) NULL COMMENT '最后消费失败信息' AFTER consumer_code;

UPDATE TXN_MESSAGE
SET original_message_id = id
WHERE original_message_id IS NULL;

ALTER TABLE TXN_MESSAGE
    MODIFY COLUMN original_message_id VARCHAR(24) NOT NULL COMMENT '原始消息id',
    MODIFY COLUMN retry_count INT NOT NULL COMMENT '已执行的消费重试次数',
    ADD UNIQUE KEY uk_txn_message_original_retry (original_message_id, retry_count);

ALTER TABLE TXN_MESSAGE_HISTORY
    ADD COLUMN original_message_id VARCHAR(24) NULL COMMENT '原始消息id' AFTER next_dispatch_time,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '已执行的消费重试次数' AFTER original_message_id,
    ADD COLUMN consumer_code VARCHAR(64) NULL COMMENT '消费者编码' AFTER retry_count,
    ADD COLUMN last_error VARCHAR(1000) NULL COMMENT '最后消费失败信息' AFTER consumer_code;

UPDATE TXN_MESSAGE_HISTORY
SET original_message_id = id
WHERE original_message_id IS NULL;

ALTER TABLE TXN_MESSAGE_HISTORY
    MODIFY COLUMN original_message_id VARCHAR(24) NOT NULL COMMENT '原始消息id',
    MODIFY COLUMN retry_count INT NOT NULL COMMENT '已执行的消费重试次数';
