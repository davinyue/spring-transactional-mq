# Task 3：API 契约质量修复

## 背景

- Task 3 已完成基础 API 模型与接口定义。
- 现需在 `spring-transactional-mq-api` 模块内继续收敛契约，修复消费者接口、序列化抽象、发送结果和 headers 暴露问题。

## 目标

- 对齐消费者接口签名。
- 提升 payload 序列化接口对泛型类型的表达能力。
- 增强发送结果的机器可读性。
- 修复消息头的可变暴露问题。

## 任务清单

- [x] 调整 `TransactionalMessageConsumer` 契约。
- [x] 增强 `MessagePayloadSerializer` 的 `Type` 反序列化能力。
- [x] 增加 `SendResult.resultCode`。
- [x] 修复 `TransactionalMessage.headers` 可变暴露。
- [x] 更新测试覆盖新契约。
- [x] 运行 `mvn -q -pl spring-transactional-mq-api -am test` 验证。

## 进度

- 已完成。
