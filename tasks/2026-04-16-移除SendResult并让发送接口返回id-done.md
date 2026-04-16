# 移除 SendResult 并让发送接口返回 id

## 背景

当前 `TransactionalMessageSender` 的实际职责是将事务消息写入数据库，而不是同步发送到 MQ。继续返回 `SendResult` 会让语义偏重“发送结果”，不够贴近实际行为。

## 需求

- 删除 `SendResult` 模型。
- `TransactionalMessageSender.send(...)` 改为直接返回消息 `id`。
- `MessagePublishService` 实现 `TransactionalMessageSender`。
- 在发送接口实现类的发送方法上增加事务注解。
- 更新相关调用点与测试。

## 任务列表

- [x] 补充接口与核心服务红测。
- [x] 移除 `SendResult` 并调整发送接口。
- [x] 调整 `MessagePublishService` 与调用点。
- [x] 执行相关测试。

## 进度

- 2026-04-16 创建任务记录。
- 2026-04-16 已完成接口简化、事务注解补充和相关验证。
