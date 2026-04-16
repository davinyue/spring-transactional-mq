# ObjectId 与字段长度调整

## 背景

当前数据库中 `id` 与 `message_id` 字段长度为 64，业务消息标识和持久化主键使用 UUID 无横线字符串生成。现在统一改为 luava-id 的 ObjectId，长度为 24 位。

## 需求

- 所有 SQL 中的 `id` 字段长度改为 24。
- 所有 SQL 中的 `message_id` 字段长度改为 24。
- 业务消息标识 `messageId` 使用 `new ObjectId().toHexString()` 生成。
- 持久化主键 `id` 自动生成时使用 `new ObjectId().toHexString()`。
- 保持已有手动传入 id 时不覆盖。

## 任务列表

- [x] 补充 ObjectId 长度红灯测试。
- [x] 调整 core 与 store ID 生成逻辑。
- [x] 调整多数据库 SQL 字段长度。
- [x] 执行相关 Maven 测试。

## 进度

- 2026-04-15 创建任务记录。
- 2026-04-15 已将 `luava-id` 依赖移动到 common 模块统一封装。
- 2026-04-15 已将业务 `messageId` 与持久化主键自动生成改为 24 位 ObjectId。
- 2026-04-15 已将各数据库 SQL 中 `id`、`message_id` 字段长度调整为 24。
- 2026-04-15 已通过单元测试、默认聚合测试和真实 Oracle/RabbitMQ 测试。
