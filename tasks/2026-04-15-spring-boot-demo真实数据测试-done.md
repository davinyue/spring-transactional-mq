# Spring Boot Demo 真实数据测试

## 背景

项目需要新增一个 Spring Boot demo 工程，用于连接真实 Oracle 数据库和 RabbitMQ，验证 starter 自动装配、事务消息入库和 RabbitMQ 真实发送链路。

## 需求

- 新增独立 Maven 模块作为 Spring Boot demo 工程。
- demo 依赖当前项目的 Spring Boot starter。
- 使用用户提供的 RabbitMQ 和 Oracle 配置。
- 默认避免误连真实环境，通过指定 profile 启动真实数据测试。
- 提供启动后自动写入一条 demo 消息并触发一次派发的能力。

## 任务列表

- [x] 新增 demo Maven 模块并加入父工程。
- [x] 新增 Spring Boot 启动类和真实数据测试 runner。
- [x] 新增真实环境配置文件。
- [x] 执行最小必要编译验证。

## 进度

- 2026-04-15 创建任务记录。
- 2026-04-15 完成 demo 模块、真实环境配置和编译验证。
