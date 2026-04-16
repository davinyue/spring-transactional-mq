# Task 3：定义 API 层模型与业务接口

## 背景

- Task 1、Task 2 已完成，`common` 模块已具备基础实体与枚举。
- 当前需要在 `spring-transactional-mq-api` 模块补齐对外契约，供后续 `core`、`store`、`rabbitmq` 和 `starter` 继续实现。

## 目标

- 定义事务消息模型、消费上下文、发送结果。
- 定义统一的生产者、消费者和 payload 序列化接口。
- 保持 JDK 1.8 兼容，避免引入实现层依赖。

## 任务清单

- [x] 新增 `TransactionalMessage<T>`。
- [x] 新增 `ConsumeContext`。
- [x] 新增 `SendResult`。
- [x] 新增 `TransactionalMessageSender`。
- [x] 新增 `TransactionalMessageConsumer<T>`。
- [x] 新增 `MessagePayloadSerializer`。
- [x] 补充最小单元测试。
- [x] 运行 `mvn -q -pl spring-transactional-mq-api test` 验证。

## 进度

- 已完成。
