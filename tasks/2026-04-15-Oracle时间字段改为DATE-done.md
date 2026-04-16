# Oracle 时间字段改为 DATE

## 背景

Oracle 的 `DATE` 类型包含年月日时分秒，当前项目 Oracle 建表脚本使用 `TIMESTAMP`，用户希望仅调整 Oracle 脚本。

## 需求

- 只修改 `ORACLE.sql`。
- 将所有时间字段从 `TIMESTAMP` 改为 `DATE`。
- 其它数据库脚本保持不变。

## 任务列表

- [x] 修改 Oracle 建表 SQL 时间字段类型。
- [x] 检查 Oracle 脚本不再包含 `TIMESTAMP`。
- [x] 执行 store 模块测试。

## 进度

- 2026-04-15 创建任务记录。
- 2026-04-15 已将 `ORACLE.sql` 中所有时间字段从 `TIMESTAMP` 改为 `DATE`，其它数据库脚本未调整。
- 2026-04-15 store 模块测试通过。
