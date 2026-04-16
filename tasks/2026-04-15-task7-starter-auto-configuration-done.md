# Task 7: starter 自动装配与配置绑定

## 背景
- 当前 core / store-ezmybatis / rabbitmq 已完成，starter 需要把主链路自动装配起来。

## 目标
- 新增 TransactionalMqProperties 和 TransactionalMqAutoConfiguration。
- 通过 spring.factories 注册自动配置。
- 补最小自动装配测试，覆盖启停和 RabbitMQ 适配器装配。

## 任务列表
- [x] 设计并实现 starter 配置属性绑定。
- [x] 实现自动装配并串起核心服务与仓储/适配器。
- [x] 补充 spring.factories。
- [x] 补充 ApplicationContextRunner 测试并验证。

## 完成进度
- 100%
