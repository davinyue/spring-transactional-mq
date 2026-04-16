# Task 5 - store-ezmybatis 实体与仓储实现

## 背景

`core` 已完成仓储 SPI 与服务骨架，当前需要在 `spring-transactional-mq-store-ezmybatis` 模块补齐默认数据库实体、仓储实现和多数据库 SQL 骨架，以支撑事务消息持久化、消费幂等和归档能力。

## 目标

- 建立 4 张核心表对应的实体类
- 基于 `EzDao` 实现默认仓储
- 补齐多数据库 SQL 建表骨架
- 增加最小单测并通过指定 Maven 验证

## 任务列表

- [x] 创建 4 个实体类
- [x] 实现 3 个默认仓储
- [x] 补齐 5 份数据库 SQL 骨架
- [x] 增加最小单测
- [x] 运行 `mvn -q -pl spring-transactional-mq-store-ezmybatis -am test`

## 完成进度

- 已完成

## 验证结果

- `mvn -q -pl spring-transactional-mq-store-ezmybatis -am test` 通过。
- 后续又通过 `mvn -q test` 聚合级回归验证。
