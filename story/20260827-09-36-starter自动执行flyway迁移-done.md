# starter 自动执行 Flyway 迁移

## 背景

此前将 Flyway 迁移执行责任完全交给业务工程，与组件需要在开启自动初始化时自行处理数据库脚本的要求不符。

## 目标

由 spring-boot-starter 创建并执行事务消息专用的 Flyway Bean，在启用自动初始化时按当前数据库类型执行对应迁移脚本。

## 范围

- 为 starter 引入 Flyway 依赖并恢复自动初始化配置。
- 创建事务消息专用 Flyway Bean 和迁移初始化器。
- 支持可选的存量库 baseline 版本配置。
- 更新测试、demo 配置和 README。

## 非目标

- 不接管业务工程已有的 Flyway 配置和迁移目录。
- 不自动推断存量数据库的 schema 版本。
- 不执行真实数据库迁移。

## 关键设计

自动初始化默认关闭。开启后，starter 使用 `EzMybatisContent` 识别数据库类型并配置唯一的 `db/migration/<database>` location。Flyway 的 schema history 表记录执行状态；仅在显式设置 baseline 版本时，对无历史表的非空库执行 baseline。

## 验收标准

- 开启自动初始化时注册事务消息专用 Flyway Bean 与迁移初始化器。
- 迁移 location 与五种数据库类型正确对应。
- 关闭自动初始化时不注册事务消息 Flyway Bean。
- 文档说明配置和 baseline 行为。

## 任务清单

### 分析

- [x] 明确需求目标、范围和非目标
- [x] 确认 Flyway Bean、数据库类型识别和现有迁移资源影响范围

### 实施

- [x] 完成 Flyway 自动配置与属性定义
- [x] 更新测试、demo 配置和文档

### 验证

- [x] 执行与改动直接相关的测试、构建或检查
- [x] 记录无法验证的原因与风险

### 收尾

- [x] 更新当前进度
- [x] 记录剩余风险与阻塞

## 当前进度

已完成 starter 内置 Flyway、自动初始化开关、数据库目录路由、可选 baseline 及文档更新。验证了开关关闭时 Spring Boot 默认 Flyway 自动配置退避，开启时会调用事务消息专用 Flyway 的 `migrate()`。

## 风险与阻塞

Spring Boot 2.7 管理的 Flyway 8.5.13 核心包不包含达梦适配器；DM 使用方需要额外提供兼容扩展及 JDBC 驱动，本项目没有真实达梦环境验证。已有 starter 的完整测试仍有 `SamplePayload` 无无参构造导致的既有序列化测试失败，与本次变更无关。
