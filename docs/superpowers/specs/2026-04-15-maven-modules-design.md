# Spring Transactional MQ Maven 模块设计

## 1. 背景与目标

当前项目计划实现一个基于 Spring Boot 的事务消息队列框架，满足以下前提：

- 支持 JDK 1.8
- 支持 Spring Boot 2.7.18
- 首版支持 RabbitMQ
- 后续需要扩展支持更多 MQ
- 对业务方主要以 `spring-boot-starter` 方式接入
- 生产端先入库，再由后台线程异步发送
- 消费端通过统一消费者接口接入，并将已消费消息 ID 入库，保证幂等消费
- 持久化层默认基于 `ez-mybatis`，兼顾多数据库支持

本设计以 Maven 模块拆分为主，同时补充已经确认的核心持久化边界约束，避免后续实现规划阶段重复调整

## 2. 设计原则

- 核心流程与具体 MQ 实现解耦
- 核心流程与具体存储实现解耦
- 业务接入面稳定，避免直接依赖底层实现细节
- 首版结构满足 RabbitMQ 落地，同时为后续扩展 Kafka、RocketMQ 预留平行扩展位
- 维持最小必要模块数，避免首版过度拆分

## 3. 候选方案结论

候选方案中，最终采用“核心能力与适配层分离”的方案 B，而不采用更极简或更细粒度的拆分：

- 方案 A 过于紧凑，`core` 容易膨胀，后续扩展更多 MQ 时边界会变差
- 方案 C 对首版而言偏重，容易在调度与核心编排之间产生过度设计
- 方案 B 在首版交付成本和后续扩展性之间平衡最好

## 4. 最终模块拆分

### 4.1 `spring-transactional-mq-parent`

父工程，负责统一管理：

- JDK 1.8 编译参数
- Spring Boot 2.7.18 相关版本
- 公共插件版本
- 模块依赖版本收敛

该模块本身不承载业务代码

### 4.2 `spring-transactional-mq-common`

公共基础模块，负责放置跨模块共享但不体现领域编排的内容，例如：

- 公共常量
- 异常定义
- 状态枚举
- 通用结果对象
- 时间、ID、序列化等基础工具或抽象

该模块不应承载事务消息主流程

### 4.3 `spring-transactional-mq-api`

对业务方暴露的稳定接口模块，负责定义业务接入面，例如：

- 事务消息发送接口
- 统一消费者接口
- 消息载体模型
- 发送结果对象
- 消费上下文对象
- 序列化接口

业务代码应尽量只依赖本模块和 starter，而不直接依赖 RabbitMQ 或持久化实现模块

### 4.4 `spring-transactional-mq-core`

事务消息核心模块，负责实现框架主流程编排，包括：

- 事务内消息创建与入库编排
- 待发送消息扫描
- 异步发送执行
- 发送重试
- 死信流转
- 消费幂等校验流程
- 消费记录写入流程
- 消息清理流程

该模块只定义抽象边界，不直接依赖 RabbitMQ 或 `EzDao`首版建议在此模块内定义至少以下 SPI：

- `TransactionalMessageRepository`
- `ConsumedMessageRepository`
- `MqProducerAdapter`

如后续需要消费者绑定抽象，也可在此模块内补充消费者适配 SPI

### 4.5 `spring-transactional-mq-store-ezmybatis`

默认存储实现模块，负责承接数据库相关实现，基于 `ez-mybatis` 和 `EzDao` 落地建议包含：

- 事务消息表实体
- 已消费消息记录主表实体
- 已消费消息记录历史表实体
- 发送历史或异常记录实体
- 基于 `EzDao` 的 repository 实现
- 按状态扫描、更新、幂等判定所需的数据库访问逻辑
- 多数据库建表 SQL 资源与必要的方言差异处理

该模块实现 `core` 中定义的 repository 接口

### 4.6 `spring-transactional-mq-rabbitmq`

RabbitMQ 适配模块，负责首版 MQ 实现，建议包含：

- `MqProducerAdapter` 的 RabbitMQ 实现
- 统一消费者接口与 RabbitMQ 监听模型之间的桥接
- 消费确认、重试、拒绝、死信等行为映射

该模块应只关注 RabbitMQ 相关逻辑，不承载核心状态机和数据库访问逻辑

### 4.7 `spring-transactional-mq-spring-boot-starter`

Spring Boot 自动装配模块，是业务方的主要接入入口，建议负责：

- 自动装配
- 配置项绑定
- 条件化 Bean 装配
- 默认线程池与调度器装配
- `core`、`store-ezmybatis`、`rabbitmq` 的组合装配
- 面向业务方的默认启用体验

业务方最终主要引入该模块

## 5. 模块依赖方向

建议采用如下依赖关系：

- `spring-transactional-mq-api` 依赖 `spring-transactional-mq-common`
- `spring-transactional-mq-core` 依赖 `spring-transactional-mq-api`、`spring-transactional-mq-common`
- `spring-transactional-mq-store-ezmybatis` 依赖 `spring-transactional-mq-core`、`spring-transactional-mq-api`、`spring-transactional-mq-common`
- `spring-transactional-mq-rabbitmq` 依赖 `spring-transactional-mq-core`、`spring-transactional-mq-api`、`spring-transactional-mq-common`
- `spring-transactional-mq-spring-boot-starter` 依赖 `spring-transactional-mq-core`、`spring-transactional-mq-store-ezmybatis`、`spring-transactional-mq-rabbitmq`

`spring-transactional-mq-parent` 作为聚合与版本管理模块，不参与业务依赖链

## 6. 为什么不直接让 core 依赖 ez-mybatis

虽然首版已经明确默认存储方案是 `ez-mybatis`，但仍不建议让 `core` 直接依赖 `EzDao`原因如下：

- 事务消息主流程属于稳定领域逻辑，数据库访问属于基础设施实现
- 如果 `core` 直接依赖 `EzDao`，后续扩展存储实现或做测试替身时边界会迅速变差
- 将 repository 接口留在 `core`，再由 `store-ezmybatis` 提供默认实现，更符合后续扩展需求

因此，`ez-mybatis` 应作为默认基础设施实现，而不是核心领域的一部分

## 7. 首版边界建议

首版建议只维持当前七个模块，不额外拆分如下模块：

- 单独的 `scheduler` 模块
- 单独的 `metrics` 模块
- 单独的 `admin` 模块

原因是首版优先目标是打通以下主链路：

- 业务发送接口
- 事务内入库
- 后台扫描与异步发送
- RabbitMQ 投递
- 消费幂等处理
- Starter 自动装配

在这些能力稳定之前，进一步拆分会增加结构复杂度而不会明显提升收益

## 8. 命名建议

建议模块命名统一采用以下 artifactId：

1. `spring-transactional-mq-parent`
2. `spring-transactional-mq-common`
3. `spring-transactional-mq-api`
4. `spring-transactional-mq-core`
5. `spring-transactional-mq-store-ezmybatis`
6. `spring-transactional-mq-rabbitmq`
7. `spring-transactional-mq-spring-boot-starter`

其中根目录工程建议直接切换为 parent 聚合工程，原根 `pom.xml` 调整为 `packaging=pom`

## 9. 后续设计入口

在模块设计确认后，下一步建议继续细化以下内容：

- 四张核心表的职责边界与字段设计
- `core` 层状态机与核心服务拆分
- 统一消费者接口定义
- RabbitMQ 适配边界
- starter 自动装配与配置项设计

这些内容适合进入下一阶段实现规划

## 10. 已确认的持久化约束

### 10.1 基础审计实体

所有持久化实体统一继承 `BaseEntity`，首版固定包含以下审计字段：

- `id`
- `createTime`
- `updateTime`

其中：

- `id` 类型固定为 `String`
- 时间字段建议使用 `java.util.Date`
- `createTime`、`updateTime` 由统一 listener 或框架层自动填充

### 10.2 核心表数量

核心表从原先建议的 3 张调整为 4 张：

1. 事务消息主表
2. 已消费消息记录主表
3. 已消费消息记录历史表
4. 消息发送历史/异常表

### 10.3 幂等窗口规则

消费幂等采用“有限时间窗口幂等”：

- 已消费消息记录主表只保留近期幂等数据
- 超过保留期的数据定时迁移到历史表
- 历史表不参与实时幂等判重
- 超过保留期的旧消息如果再次投递，视为新消息重新消费

该规则用于保证主表性能边界稳定，而不是提供永久幂等
