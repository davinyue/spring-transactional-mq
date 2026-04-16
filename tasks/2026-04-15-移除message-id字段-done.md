# 移除 message_id 字段

## 背景

当前模型中同时存在持久化主键 `id` 与业务消息标识 `messageId`，两者都使用 24 位 ObjectId，语义重复。后续统一使用 `id` 作为消息唯一标识，去除 `message_id` 字段和 Java 侧 `messageId` 属性。

## 需求

- 移除所有表中的 `message_id` 字段。
- Java API、核心模型、仓储实体和测试统一使用 `id`。
- RabbitMQ 发送属性中的 messageId 使用消息 `id` 填充，消费上下文使用 `id` 承载。
- 消费幂等唯一约束从 `(message_id, consumer_code)` 调整为 `(id, consumer_code)`，发送日志唯一约束使用 `id`。
- 更新 README 和关联建表 SQL。

## 任务列表

- [x] 先调整测试表达目标行为并确认失败。
- [x] 移除 API/Core/Store/RabbitMQ 中的 `messageId` 字段和映射。
- [x] 更新 5 套数据库 SQL。
- [x] 更新 README。
- [x] 执行模块测试和真实环境测试。

## 进度

- 2026-04-15 创建任务记录。
- 2026-04-15 已完成字段移除、SQL 更新和真实环境验证。
