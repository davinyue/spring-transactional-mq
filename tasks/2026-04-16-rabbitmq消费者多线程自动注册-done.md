# rabbitmq 消费者多线程自动注册

## 背景

当前项目只有统一消费者接口和调用器，还没有真正的 RabbitMQ 消费容器注册层。需要支持业务定义多个消费者 Bean，并为每个消费者按接口配置的队列名和并发范围自动注册 `SimpleMessageListenerContainer`。

## 需求

- `TransactionalMessageConsumer` 增加 `getQueueName()`、`getMinConcurrency()`、`getMaxConcurrency()`。
- RabbitMQ 模块新增消费者注册器和消息监听器，支持手动 ack。
- starter 自动扫描并注册容器内的消费者 Bean。
- 消费链路接入现有反序列化、幂等记录和消费者调用。
- 执行相关模块测试。

## 任务列表

- [x] 补充接口与自动注册红测。
- [x] 扩展消费者接口。
- [x] 实现 RabbitMQ 自动注册器和监听器。
- [x] 补充 starter 自动装配。
- [x] 执行相关测试。

## 进度

- 2026-04-16 创建任务记录。
- 2026-04-16 已完成消费者并发接口、自动注册器、监听器和相关测试。
