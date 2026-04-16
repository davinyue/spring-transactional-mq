# Task 7 - RabbitMQ 首版发送适配质量修复

## 背景

Task 6 已实现 RabbitMQ 首版发送适配，但需要收紧 `destination` 约定并修复保留头被业务头覆盖的问题。

## 目标

- 收紧 `destination` 的 RabbitMQ 约定
- 确保框架保留头最终不被业务 headers 覆盖
- 补充最小单元测试并验证

## 任务列表

- [x] 修正 `RabbitMqProducerAdapter`
- [x] 更新单元测试
- [x] 运行 `mvn -q -pl spring-transactional-mq-rabbitmq -am test`

## 完成进度

- 已完成
