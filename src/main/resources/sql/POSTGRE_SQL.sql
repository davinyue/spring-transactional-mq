CREATE TABLE rd_db_atomic
(
    id           VARCHAR(64) PRIMARY KEY,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP,
    int_value    INTEGER,
    long_value   BIGINT,
    double_value NUMERIC(17, 6)
);
