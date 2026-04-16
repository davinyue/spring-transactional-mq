CREATE TABLE rd_db_atomic
(
    id           VARCHAR(64) PRIMARY KEY,
    create_time  DATETIME,
    update_time  DATETIME,
    int_value    INT,
    long_value   BIGINT,
    double_value DECIMAL(17, 6)
);
