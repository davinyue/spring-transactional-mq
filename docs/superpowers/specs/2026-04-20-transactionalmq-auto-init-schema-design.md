# 事务消息自动建表设计

## 背景

当前 `spring-transactional-mq-store-ezmybatis` 提供了多数据库建表 SQL，但框架本身不会根据 `ez-mybatis` 的数据库类型自动选择并执行建表脚本。业务接入时仍需要手工指定 `schema-locations` 或自行执行 SQL，接入成本较高。

## 目标

- 在框架启动时默认自动尝试建表。
- 基于 ez-mybatis 的数据库配置动态识别数据库类型。
- 自动选择匹配数据库类型的建表 SQL 并执行。
- 建表失败不影响应用启动，异常直接吞掉。

## 非目标

- 本次不做跨数据库“表是否存在”探测。
- 本次不做建表失败重试。
- 本次不额外输出建表失败日志。
- 本次不改动现有仓储和消息主流程。

## 方案对比

### 方案一：启动时检查核心表，缺表时执行匹配脚本并吞掉异常

- 通过 ez-mybatis `Configuration` 调用 `EzMybatisContent.getDbType(Configuration)` 获取 `DbType`。
- 通过 `EzMapper.tableExists(EntityTable.of(实体类))` 判断 5 张事务消息核心表是否存在。
- 全部存在时跳过建表。
- 按 `DbType` 映射到 `classpath:sql/*.sql`。
- 使用 `ResourceDatabasePopulator` 执行脚本。
- 使用 `try/catch` 包裹初始化过程，异常直接吞掉。

优点：

- 改动最小。
- 满足“默认开启自动建表”的要求。
- 避免在表已存在时反复执行建表脚本。
- 不影响现有应用启动可用性。

缺点：

- 建表失败时没有显式反馈，只能由用户自行排查表缺失问题。

### 方案二：先检查表是否存在，不存在再执行脚本

- 启动时先查询关键表是否存在。
- 仅在缺表时执行建表脚本。

优点：

- 重复启动更干净。

缺点：

- 需要补充多数据库表存在性判断，复杂度明显更高。
- 与本次“最小改动”目标不匹配。

## 采用方案

采用方案一。

## 详细设计

### 配置项

在 `TransactionalMqProperties` 新增：

- `autoInitSchema`
  - 配置前缀：`transactionalmq.auto-init-schema`
  - 默认值：`true`

### 初始化器职责

新增一个独立的 schema 初始化器，职责仅包括：

- 获取 ez-mybatis `Configuration`
- 获取 `EzMapper`
- 判断核心表是否已存在
- 解析数据库类型
- 选择 SQL 脚本
- 执行建表脚本
- 吞掉所有异常

不把这部分逻辑直接塞进 `TransactionalMqAutoConfiguration`，避免配置类膨胀。

### 数据库类型与脚本映射

- `MYSQL` -> `classpath:sql/MYSQL.sql`
- `ORACLE` -> `classpath:sql/ORACLE.sql`
- `DM` -> `classpath:sql/DM.sql`
- `POSTGRE_SQL` -> `classpath:sql/POSTGRE_SQL.sql`
- `SQL_SERVER` -> `classpath:sql/SQL_SERVER.sql`

如果遇到未支持的 `DbType`，直接不执行，保持吞异常策略的一致性。

### 启动时机

在 starter 自动装配阶段注册初始化器 Bean，并在 Bean 初始化后执行建表逻辑。

触发条件：

- `transactionalmq.enabled=true`
- `transactionalmq.auto-init-schema=true`
- 容器中存在 `EzDao`
- 容器中存在 `EzMapper`
- 能拿到 ez-mybatis 的 `Configuration`

### 异常策略

- 整个初始化过程使用 `try/catch`
- 不打印异常日志
- 不向外抛异常
- 不阻断 Spring 容器启动

## 测试设计

### 单元/自动装配测试

需要覆盖：

- 默认配置下会注册自动建表初始化器
- `transactionalmq.auto-init-schema=false` 时不注册或不执行
- 5 张核心表都存在时不执行脚本
- 任一核心表缺失时执行脚本
- 不同 `DbType` 选择正确的 SQL 路径
- 建表过程抛异常时不会影响上下文启动

### 验证重点

- 配置项默认值正确
- 自动装配条件正确
- 吞异常行为符合预期

## 风险

- 建表失败完全静默，用户只能在后续访问表时报错后自行排查。
- 如果底层 ez-mybatis 提供的 `DbType` 枚举名称与当前预期不一致，需要在实现阶段按真实 API 做映射修正。
