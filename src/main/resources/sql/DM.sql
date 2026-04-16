CREATE TABLE rd_db_atomic
(
    id           VARCHAR2(64) PRIMARY KEY,
    create_time  DATE,
    update_time  DATE,
    int_value    NUMBER(10),
    long_value   NUMBER(19),
    double_value NUMBER(17, 6)
);
