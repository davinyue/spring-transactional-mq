# 事务消息历史表更名为 TXN_MESSAGE_HISTORY

## 背景

当前事务消息历史表名为 `TXN_MESSAGE_HIS`，命名不够直观。需要统一改为 `TXN_MESSAGE_HISTORY`，并验证真实环境测试链路。

## 需求

- 将事务消息历史表名从 `TXN_MESSAGE_HIS` 改为 `TXN_MESSAGE_HISTORY`。
- 更新实体映射、5 套 SQL、README 与真实测试中的表名引用。
- 执行真实环境测试验证。

## 任务列表

- [x] 更新实体与 SQL。
- [x] 更新 README 与测试中的表名引用。
- [x] 执行真实环境测试。

## 进度

- 2026-04-16 创建任务记录。
- 2026-04-16 已完成表名切换并通过真实环境测试。
