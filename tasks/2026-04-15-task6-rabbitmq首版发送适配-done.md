# Task 6 - RabbitMQ 首版发送适配

## 背景

`core` 已完成 `DispatchMessage`、`MqProducerAdapter`、`MessageDispatchService`，`api` 也已提供统一消费者契约。当前需要在 `spring-transactional-mq-rabbitmq` 模块补齐最小可用的 RabbitMQ 发送适配与消费调用桥接。

## 目标

- 实现 RabbitMQ 生产者适配器
- 实现 RabbitMQ 消费者调用器
- 补充最小单元测试
- 运行指定 Maven 验证

## 任务列表

- [x] 创建 `RabbitMqProducerAdapter`
- [x] 创建 `RabbitMqConsumerInvoker`
- [x] 补充最小测试依赖
- [x] 增加单元测试
- [x] 运行 `mvn -q -pl spring-transactional-mq-rabbitmq -am test`

## 完成进度

- 已完成
