# spring-transactional-mq

`spring-transactional-mq` 是一个面向 Spring Boot 的事务消息队列组件。它将业务发送请求先落库，再由派发线程把消息发送到 MQ；消费侧在业务消费者执行前记录已消费消息，依赖数据库唯一约束实现消费幂等。

当前版本以 Spring Boot Starter 为主，支持 JDK 1.8、Spring Boot 2.7.18、RabbitMQ 和 ez-mybatis 存储。RocketMQ、Kafka 暂未实现适配器，但核心发送模型已经按多 MQ 兼容方式抽象。

## 核心能力

- 生产者调用统一接口发布事务消息，消息先写入数据库。
- 派发线程领取待发送消息并发送到 MQ，发送结果写入日志表。
- 多实例部署时通过数据库条件更新抢占消息，避免多个实例同时派发同一条待发送记录。
- 消费者通过统一接口消费消息，消费前写入已消费记录，重复消息会被数据库唯一约束拦截。
- 事务消息发布时写入主表，发送成功后可定期迁移到历史表并清理主表。
- 消费记录同时写入主表和历史表，主表可按保留天数清理，降低高频去重表的数据量。
- 消息唯一标识统一使用 `id`，由 `luava-id` 的 `new ObjectId().toHexString()` 生成，长度为 24 位。

## 模块说明

| 模块 | 说明 |
| --- | --- |
| `spring-transactional-mq-common` | 公共枚举、BaseEntity、ObjectId 生成器。 |
| `spring-transactional-mq-api` | 对业务暴露的消息模型、发送接口、消费接口、序列化接口。 |
| `spring-transactional-mq-core` | 发布、派发、消费幂等、消费记录归档等核心服务和仓储 SPI。 |
| `spring-transactional-mq-store-ezmybatis` | 基于 ez-mybatis 的数据库仓储实现和多数据库建表 SQL。 |
| `spring-transactional-mq-rabbitmq` | RabbitMQ 生产者适配器和消费者调用器。 |
| `spring-transactional-mq-spring-boot-starter` | Spring Boot 自动装配入口。 |
| `spring-transactional-mq-demo` | 真实 Oracle/RabbitMQ 链路测试 demo。 |

## 快速接入

业务工程优先引入 starter：

```xml
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

starter 会在检测到 `EzDao` 时注册 ez-mybatis 仓储实现，在检测到 `RabbitTemplate` 时注册 RabbitMQ 适配器。业务工程仍需要正常配置数据源、ez-mybatis 和 RabbitMQ。

```yaml
transactional:
  mq:
    enabled: true
    dispatch-batch-size: 100
    consume-record-retention-days: 7

ez-mybatis:
  db-type: ORACLE

spring:
  rabbitmq:
    host: your-rabbitmq-host
    port: 5672
    virtual-host: /
    username: your-username
    password: your-password
```

当前 starter 提供 `MessagePublishService`、`MessageDispatchService`、`TransactionalMessageCleanupService`、`ConsumeIdempotentService`、`ConsumedMessageArchiveService` 等 Bean。派发任务、事务消息主表清理任务和消费记录归档任务目前由业务系统自行用定时任务或线程池调用，后续可以继续扩展为 starter 内置调度。

## 发送消息

```java
@Service
public class OrderMessageService {

    private final MessagePublishService messagePublishService;

    public OrderMessageService(MessagePublishService messagePublishService) {
        this.messagePublishService = messagePublishService;
    }

    public SendResult sendOrderPaidMessage(String orderId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("event", "ORDER_PAID");

        TransactionalMessage<Map<String, Object>> message =
                new TransactionalMessage<Map<String, Object>>()
                        .setMessageKey(orderId)
                        .setProducerCode("order-service")
                        .setMqType(MqType.RABBITMQ)
                        .setDestination("order.exchange")
                        .setRoute("order.paid")
                        .setShardingKey(orderId)
                        .setBizKey(orderId)
                        .setPayload(payload);

        return this.messagePublishService.publish(message);
    }
}
```

`publish` 只负责把消息写入事务消息表并返回 `SendResult`，真正发送 MQ 由派发线程调用 `MessageDispatchService` 完成。

```java
int dispatched = messageDispatchService.dispatchPendingMessages(100);
```

发送成功后可以定期归档 `TXN_MESSAGE` 主表中的成功消息。归档会先通过状态抢占把成功消息迁移到 `TXN_MESSAGE_HISTORY`，再删除主表记录，多实例部署时只有抢占成功的实例会执行迁移。

```java
Date cleanupBefore = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
int deleted = transactionalMessageCleanupService.cleanupSuccessMessages(cleanupBefore, 1000);
```

## 消费消息

业务消费者实现 `TransactionalMessageConsumer<T>`：

```java
public class OrderPaidConsumer implements TransactionalMessageConsumer<Map<String, Object>> {

    @Override
    public String consumerCode() {
        return "order-paid-consumer";
    }

    @Override
    public void consume(ConsumeContext context, Map<String, Object> payload) {
        // 执行业务消费逻辑
    }
}
```

消费入口需要先调用 `ConsumeIdempotentService#recordIfAbsent`。返回 `true` 表示首次消费，可以继续执行业务逻辑；返回 `false` 表示重复消息，应直接跳过。

```java
boolean firstConsume = consumeIdempotentService.recordIfAbsent(context);
if (firstConsume) {
    consumer.consume(context, payload);
}
```

RabbitMQ 模块提供 `RabbitMqConsumerInvoker` 用于调用消费者接口，但当前还没有内置 Rabbit Listener 注册器。业务系统可以在自己的 listener 中完成消息反序列化、构造 `ConsumeContext`、幂等记录和消费者调用。

## 统一发送目标抽象

核心模型不直接绑定 RabbitMQ 的 exchange/routingKey，而是使用更中性的字段：

| 字段 | RabbitMQ | RocketMQ 预期映射 | Kafka 预期映射 |
| --- | --- | --- | --- |
| `destination` | exchange 或默认 exchange 下的 queue/routingKey | topic | topic |
| `route` | routingKey | tag | 可写入 header 或忽略 |
| `messageKey` | header/message property | keys | record key |
| `shardingKey` | 通常不用 | queue selector 参数 | partition key |
| `headers` | message headers | user properties | headers |

RabbitMQ 适配器优先使用新写法：

```java
.setDestination("order.exchange")
.setRoute("order.paid")
```

为了兼容早期用法，仍支持：

```java
.setDestination("order.exchange:order.paid")
```

如果 `destination` 不包含 `:` 且没有配置 `route`，RabbitMQ 适配器会使用默认 exchange，并把 `destination` 当作 routingKey。

## 数据库表

建表 SQL 位于 `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/`，当前提供：

- `MYSQL.sql`
- `ORACLE.sql`
- `DM.sql`
- `POSTGRE_SQL.sql`
- `SQL_SERVER.sql`

核心表如下：

| 表名 | 说明 |
| --- | --- |
| `TXN_MESSAGE` | 事务消息主表，保存待派发消息、派发状态、下次派发时间。 |
| `TXN_MESSAGE_HISTORY` | 事务消息历史表，成功消息归档时写入，用于发送审计。 |
| `TXN_MESSAGE_SEND_LOG` | 消息发送日志表，记录发送成功或失败结果。 |
| `TXN_CONSUMED_MESSAGE` | 已消费消息主表，用于在线去重。 |
| `TXN_CONSUMED_MESSAGE_HISTORY` | 已消费消息历史表，用于保留消费审计记录。 |

`TXN_MESSAGE` 派发状态目前包括初始化、发送中、重试中、成功和归档中等状态。派发线程先批量查询候选消息，再按 `id` 稳定排序，逐条通过数据库条件更新抢占消息；只有成功把记录更新为 `SENDING` 并写入 `dispatch_token` 的实例才会发送该消息。发送完成后按 `id + dispatch_token` 批量落成功或失败状态，避免旧线程在租约过期后误更新新线程抢占的消息。`SENDING` 会设置锁超时时间，实例宕机后消息可在超时后重新领取。`TransactionalMessageCleanupService` 会把达到清理阈值的成功消息抢占为 `ARCHIVING`，写入 `TXN_MESSAGE_HISTORY` 后删除主表记录。

## 投递语义

当前实现提供“至少一次投递”语义。它可以避免多实例并发时同时抢到同一条待发送记录，但如果实例已经把消息发送到 MQ，随后在标记数据库 `SUCCESS` 前宕机，锁超时后仍可能再次发送。因此消费者侧必须保持幂等，建议始终使用 `ConsumeIdempotentService` 记录消费。

## 真实环境 demo

demo 工程支持真实 Oracle 和 RabbitMQ 链路测试。真实连接信息放在 `spring-transactional-mq-demo/src/main/resources/application-real.yml`，使用前请按本地测试环境修改，不建议提交生产凭据。

运行真实测试：

```bash
mvn -q -pl spring-transactional-mq-demo -am \
  -Dtest=TransactionalMqRealSendConsumeTest \
  -DrealMqTest=true \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

运行 demo 应用并启用真实链路 runner：

```bash
mvn -pl spring-transactional-mq-demo -am spring-boot:run -Dspring-boot.run.profiles=real
```

## 本地验证

运行默认测试：

```bash
mvn -q -pl spring-transactional-mq-demo -am test
```

运行 RabbitMQ 抽象相关测试：

```bash
mvn -q -pl spring-transactional-mq-rabbitmq -am \
  -Dtest=TransactionalMessageApiTest,MessagePublishServiceTest,MessageDispatchServiceTest,RabbitMqProducerAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

运行 ObjectId 和存储映射相关测试：

```bash
mvn -q -pl spring-transactional-mq-store-ezmybatis -am \
  -Dtest=ObjectIdGeneratorTest,MessagePublishServiceTest,TransactionalMessageEntityMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## 当前限制与后续方向

- 当前仅实现 RabbitMQ 适配器，RocketMQ 和 Kafka 需要新增独立模块。
- 当前没有内置自动派发和清理调度器，业务系统需要自行调度 `MessageDispatchService`、`TransactionalMessageCleanupService`。
- 当前没有内置 Rabbit Listener 注册器，消费入口需要业务系统结合自己的 listener 接入。
- 派发锁超时时间和失败重试延迟目前在 ez-mybatis 实现中为固定值，后续可抽到 starter 配置。
- exactly-once 不是当前目标，消费者幂等是必要设计前提。
