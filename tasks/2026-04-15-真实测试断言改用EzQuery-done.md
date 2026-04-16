# 真实测试断言改用 EzQuery

## 背景

真实环境测试的数据清理已经改为 `EzDelete`，但断言查询仍使用 `JdbcTemplate` 手写 SQL。

## 需求

- 将真实环境测试中的断言查询改为 `EzQuery`。
- 移除真实环境测试中的 `JdbcTemplate` 注入。
- 保持真实 Oracle/RabbitMQ 测试通过。

## 任务列表

- [x] 将 message 状态查询改为 `EzDao.queryOne(EzQuery)`。
- [x] 将数量断言改为 `EzDao.queryCount(EzQuery)`。
- [x] 移除 `JdbcTemplate` 依赖。
- [x] 执行 demo 默认测试和真实环境测试。

## 进度

- 2026-04-15 创建任务记录。
- 2026-04-15 已将真实测试断言查询全部改为 `EzQuery`，并通过 demo 默认测试和真实环境测试。
