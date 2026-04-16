# Task 7: starter store 默认实现串接修复

## 背景
- starter 需要在有 `EzDao` 和 `RabbitTemplate` 的上下文里，自动装配 store 默认实现并串起核心服务。

## 目标
- 显式注册 ez-mybatis 默认仓储实现。
- 让核心服务在不手动声明 repository bean 的情况下自动装配。
- 将测试切换为只提供底层基础设施 bean。

## 任务列表
- [x] 在 starter auto-configuration 中补齐默认 repository bean。
- [x] 调整自动装配测试，移除手工 repository 前置条件。
- [x] 运行 starter 模块测试验证。

## 完成进度
- 100%
