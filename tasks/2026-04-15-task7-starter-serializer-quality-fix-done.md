# Task 7: starter 默认 serializer 与入口护栏修复

## 背景
- starter 当前默认 `MessagePayloadSerializer` 过于弱，且入口测试只覆盖了直接配置类 happy path。

## 目标
- 用 Jackson 实现可逆且类型安全更好的默认 serializer。
- 增强 starter 入口测试，覆盖 `spring.factories` 注册与默认 serializer 覆盖关系。

## 任务列表
- [x] 在 starter 中实现 Jackson 默认 serializer。
- [x] 补齐最小必要依赖与配置注入。
- [x] 增强自动装配测试，覆盖 serializer 往返、覆盖关系和 spring.factories 入口。
- [x] 运行 starter 模块测试验证。

## 完成进度
- 100%
