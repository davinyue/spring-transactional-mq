# Task 4: core 层 SPI 与领域服务骨架

## 背景

当前 API 与 common 层已完成，core 模块需要定义仓储 SPI、MQ 适配 SPI 以及领域服务骨架，作为后续 store / rabbitmq 实现的承接层。

## 目标

- 定义消息、消费记录、发送日志的 repository SPI
- 定义 MQ 生产者适配 SPI
- 提供消息发布、派发、消费判重、归档的领域服务骨架
- 保持 core 不直接依赖 EzDao 或 RabbitMQ

## 任务列表

- [x] 创建 core 模块源码结构
- [x] 定义必要的 record/model 对象
- [x] 定义 repository SPI 与 MQ SPI
- [x] 定义领域服务骨架
- [x] 补充最小单元测试
- [x] 补齐并验证 core 模块测试依赖

## 进度

- 已完成
