# spring-transactional-mq

`spring-transactional-mq` 是一个面向 Spring Boot 的事务消息组件。业务侧先把消息写入数据库，再由后台派发线程异步投递到
MQ；消费侧在执行业务逻辑前记录消费记录，通过数据库唯一约束实现幂等控制。

它更适合“本地事务成功后，再可靠地把事件投递到 MQ”这类场景，例如订单创建、支付完成、库存变更后的异步通知。

## 适用场景

- 业务数据和消息投递需要先后有序，要求先落本地事务，再异步发 MQ
- 可以接受 MQ 层可能重复投递到消费端
- 接受由框架消费记录表拦截重复消息，避免重复进入业务消费逻辑
- 使用 Spring Boot 2.7.x，且当前存储实现采用 `ez-mybatis`

## 不适用场景

- 需要 exactly-once 语义
- 不希望引入数据库表做消息暂存、发送日志和消费去重
- 不使用 Spring Boot，或者不准备接入 `ez-mybatis`

## 当前支持

### MQ 支持

- RabbitMQ
- Kafka

### 存储支持

- 当前仓储实现为 `ez-mybatis`
- 建表 SQL 提供 `MySQL`、`Oracle`、`DM`、`PostgreSQL`、`SQL Server`

### 运行特性

- 支持多 MQ 生产路由，发送时按 `MqType` 选择具体适配器
- 消费者按 `getSupportMqType()` 自动归属到对应 MQ 注册器
- starter 内置后台派发线程
- starter 内置成功消息清理任务和消费记录清理任务

## 模块说明

| 模块                                            | 说明                           |
|-----------------------------------------------|------------------------------|
| `spring-transactional-mq-common`              | 公共枚举、基础实体、ID 生成器             |
| `spring-transactional-mq-api`                 | 对业务暴露的消息模型、生产/消费接口、序列化接口     |
| `spring-transactional-mq-core`                | 消息发布、消息派发、生产路由、消费幂等、清理服务     |
| `spring-transactional-mq-store-ezmybatis`     | 基于 `ez-mybatis` 的仓储实现和建表 SQL |
| `spring-transactional-mq-rabbitmq`            | RabbitMQ 生产者适配器、消费者自动注册与监听处理 |
| `spring-transactional-mq-kafka`               | Kafka 生产者适配器、消费者自动注册与监听处理    |
| `spring-transactional-mq-spring-boot-starter` | Spring Boot 自动装配入口           |
| `spring-transactional-mq-demo`                | demo 工程与真实环境链路测试示例           |

## 快速接入

### 1. 引入依赖

业务项目至少需要引入 starter 和你要使用的 MQ 模块。

RabbitMQ 场景：

```xml
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-rabbitmq</artifactId>
    <version>0.0.5</version>
</dependency>
```

Kafka 场景：

```xml
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-kafka</artifactId>
    <version>0.0.5</version>
</dependency>
```

说明：

- `spring-transactional-mq-spring-boot-starter` 中对 RabbitMQ/Kafka 模块使用的是 `optional` 依赖，业务项目需要显式引入
- 如果要发 RabbitMQ，需要正常配置 `spring.rabbitmq.*`
- 如果要发 Kafka，需要正常配置 `spring.kafka.*`
- 当前仓储实现依赖 `EzDao`，因此业务项目需要正常接入 `ez-mybatis`

### 2. starter 自动执行 Flyway 数据库迁移

starter 内置 Flyway 依赖。开启 `transactionalmq.auto-init-schema` 后，starter 会在应用启动时内部创建事务消息专用 Flyway 并执行未完成的迁移；迁移失败会阻止应用启动，不再吞掉异常。该 Flyway 不注册为 Spring Bean，因此不会覆盖业务工程自己的 Flyway。关闭时不会执行事务消息迁移。

迁移脚本位于 `spring-transactional-mq-store-ezmybatis/src/main/resources/transactionalmq/db/migration`，不使用 Spring Boot 默认的 `classpath:db/migration`：

| 数据库 | Flyway location |
| --- | --- |
| MySQL | `classpath:transactionalmq/db/migration/mysql` |
| Oracle | `classpath:transactionalmq/db/migration/oracle` |
| DM | `classpath:transactionalmq/db/migration/dm` |
| PostgreSQL | `classpath:transactionalmq/db/migration/postgresql` |
| SQL Server | `classpath:transactionalmq/db/migration/sqlserver` |

每个目录使用相同的不可变版本序列：

- `V1__init.sql`：事务消息表初始结构。
- `V2__consume_retry.sql`：消费重试字段、数据回填及唯一键。

starter 使用 `ez-mybatis` 识别当前数据库类型，并自动选择表中的唯一一个 location；业务工程无需额外配置 `spring.flyway.locations`。自动初始化默认关闭，启用 Oracle 示例：

```yaml
transactionalmq:
  auto-init-schema: true
```

事务消息使用独立历史表 `txn_mq_flyway_schema_history`，可通过 `schema-history-table` 修改。空库会按 V1、V2 顺序创建到最新结构。已有非空数据库且尚未有该历史表时，必须显式声明基线版本：未创建事务消息表配置 `schema-baseline-version: "0"`，未包含消费重试字段配置 `"1"`，已包含该字段配置 `"2"`。baseline 仅在配置该项时启用。

核心表如下：

| 表名                             | 说明             |
|--------------------------------|----------------|
| `TXN_MESSAGE`                  | 事务消息主表，保存待派发消息 |
| `TXN_MESSAGE_HISTORY`          | 成功消息及消费死信历史表   |
| `TXN_MESSAGE_SEND_LOG`         | 发送日志表          |
| `TXN_CONSUMED_MESSAGE`         | 在线消费去重表        |
| `TXN_CONSUMED_MESSAGE_HISTORY` | 消费记录历史表        |

### 3. 配置参数

最小配置示例：

```yaml
transactionalmq:
  enabled: true
  auto-init-schema: false
  # 存量库按实际结构选择 0、1 或 2；空库无需配置
  # schema-baseline-version: "2"
  # schema-history-table: txn_mq_flyway_schema_history
  dispatch-batch-size: 100
  dispatch-idle-sleep-millis: 30000
  success-message-retention-days: 7
  consume-record-retention-days: 7
  success-message-cleanup-batch-size: 500
  success-message-cleanup-cron: "0 30 2 * * ?"
  consume-record-cleanup-batch-size: 500
  consume-record-cleanup-cron: "0 0 3 * * ?"

ez-mybatis:
  db-type: ORACLE
```

RabbitMQ 额外配置示例：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

Kafka 额外配置示例：

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
      group-id: demo-group
```

## 自动装配说明

当 `transactionalmq.enabled=true` 时，starter 会按条件自动装配：

- `MessagePublishService`
- `MessageDispatchService`
- `TransactionalMessageDispatchScheduler`
- `ConsumeIdempotentService`
- `TransactionalMessageCleanupService`
- `ConsumedMessageCleanupService`
- RabbitMQ 或 Kafka 的生产者适配器
- RabbitMQ 或 Kafka 的消费者自动注册器

判断条件大致如下：

- 有 `EzDao` 时装配默认仓储实现
- 存在 `DataSource` 和 `EzDao` 且开启 `transactionalmq.auto-init-schema` 时，注册事务消息 Flyway 迁移执行器
- 有 RabbitMQ 相关类和连接工厂时装配 RabbitMQ 发送与消费
- 有 Kafka 相关类和消费者工厂时装配 Kafka 发送与消费

## 数据库迁移说明

- starter 内部创建的事务消息 Flyway 使用 `classpath:transactionalmq/db/migration/<数据库>` 和独立历史表 `txn_mq_flyway_schema_history`，不会注册为 Spring `Flyway` Bean，也不会读取 `spring.flyway.*`。
- starter 在业务工程没有显式配置 `spring.flyway.enabled` 时，默认关闭 Spring Boot 的 Flyway 自动配置，避免仅引入 starter 就启动一条空的默认迁移链。业务工程自身使用 Flyway 时，设置 `spring.flyway.enabled=true`，并按原有方式配置 `spring.flyway.locations`、默认历史表等；两条迁移链可同时运行。
- 业务工程仍需要正常接入 `ez-mybatis`，使容器中存在 `EzDao`。
- `transactionalmq.auto-init-schema` 默认值为 `false`；开启后应确保当前数据库可被所使用 Flyway 版本识别。
- 多数据库场景下，starter 仅使用当前数据源对应的一个 migration location，不会同时加载不同方言的目录。
- 当前 Spring Boot 2.7 管理的 Flyway 8.5.13 核心包不包含达梦适配器；使用 DM 时需要在应用中额外提供与该 Flyway 版本兼容的达梦 Flyway 扩展及 JDBC 驱动。
- 已发布的迁移脚本不可修改；后续结构变更必须新增更高版本的脚本。

## 发送消息

业务侧统一通过 `MessagePublishService` 发送。

RabbitMQ 示例：

```java
@Service
public class OrderEventPublisher {

    private final MessagePublishService messagePublishService;

    public OrderEventPublisher(MessagePublishService messagePublishService) {
        this.messagePublishService = messagePublishService;
    }

    public String publishOrderCreated(String orderId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("event", "ORDER_CREATED");

        TransactionalMessage<Map<String, Object>> message =
                new TransactionalMessage<Map<String, Object>>()
                        .setMessageKey(orderId)
                        .setProducerCode("order-service")
                        .setDestination("order.exchange")
                        .setRoute("order.created")
                        .setBizKey(orderId)
                        .setPayload(payload);

        return this.messagePublishService.send(MqType.RABBITMQ, message);
    }
}
```

Kafka 示例：

```java
public String publishUserCreated(String userId) {
    TransactionalMessage<String> message = new TransactionalMessage<String>()
            .setMessageKey(userId)
            .setProducerCode("user-service")
            .setDestination("user.created.topic")
            .setRoute(userId)
            .setBizKey(userId)
            .setPayload("{\"userId\":\"" + userId + "\"}");

    return this.messagePublishService.send(MqType.KAFKA, message);
}
```

说明：

- `send` 负责把消息写入事务消息表，不是同步直发 MQ
- 真正投递由后台派发线程完成
- 如果当前 `MqType` 没有对应适配器，会抛出 `unsupported mqType`

### destination / route 约定

RabbitMQ：

- 推荐写法：`destination=exchange`，`route=routingKey`
- 兼容写法：`destination=exchange:routingKey`
- 如果 `destination` 不含 `:` 且没有 `route`，会按默认 exchange + `destination` 作为 routingKey 发送

Kafka：

- `destination` 表示 topic
- `route` 会作为 Kafka record key

## 消费消息

业务消费者实现 `TransactionalMessageConsumer<T>` 即可，starter 会自动扫描并按 `getSupportMqType()` 注册到对应 MQ。

RabbitMQ 消费者示例：

```java
@Component
public class OrderCreatedConsumer implements TransactionalMessageConsumer<Map<String, Object>> {

    @Override
    public String getQueueName() {
        return "order.created.queue";
    }

    @Override
    public MqType getSupportMqType() {
        return MqType.RABBITMQ;
    }

    @Override
    public String consumerCode() {
        return "order-created-consumer";
    }

    @Override
    public void consume(ConsumeContext context, ConsumeHandleContext handleContext,
                        Map<String, Object> payload) {
        // 处理业务逻辑
    }
}
```

Kafka 消费者示例：

```java
@Component
public class UserCreatedConsumer implements TransactionalMessageConsumer<String> {

    @Override
    public String getQueueName() {
        return "user.created.topic";
    }

    @Override
    public MqType getSupportMqType() {
        return MqType.KAFKA;
    }

    @Override
    public String consumerCode() {
        return "user-created-consumer";
    }

    @Override
    public void consume(ConsumeContext context, ConsumeHandleContext handleContext, String payload) {
    }
}
```

说明：

- `getQueueName()` 表示 RabbitMQ 的队列名，或 Kafka 的 topic 名
- `getSupportMqType()` 用于声明该消费者属于哪个 MQ
- `beforeTransaction(...)` 在消费幂等记录和业务事务开启前执行，适合放置明确需要事务外语义的预处理逻辑
- 进入消费事务后，框架会先记录消费记录，重复消息会被幂等逻辑拦截
- `beforeTransaction(...)` 位于幂等判断之前，不能保证消费只触发一次；重复投递、重试或消费记录清理后再次投递时都可能再次执行，逻辑应具备幂等或可重复执行能力
- 消费逻辑运行在事务中，可通过 `ConsumeHandleContext` 控制提交或回滚
- 消费者抛出异常时，默认使用 `ConsumeRetryPolicy.nativeNack()` 通过 MQ 原生机制重试

### 消费失败退避重试

消费者可覆盖 `getConsumeRetryPolicy()`，为当前消费者单独声明重试策略。例如首次失败后依次等待 2、4、8、16、32 分钟和 1、2、4 小时：

```java
@Override
public ConsumeRetryPolicy getConsumeRetryPolicy() {
    return ConsumeRetryPolicy.customDelays(
            Duration.ofMinutes(2),
            Duration.ofMinutes(4),
            Duration.ofMinutes(8),
            Duration.ofMinutes(16),
            Duration.ofMinutes(32),
            Duration.ofHours(1),
            Duration.ofHours(2),
            Duration.ofHours(4));
}
```

消费方法抛出异常，或通过处理上下文要求回滚且不提前确认 MQ 消息时，会进入上述重试策略：

```java
@Override
public void consume(ConsumeContext context, ConsumeHandleContext handleContext,
                    OrderCreatedMessage payload) {
    if (!orderService.create(payload)) {
        handleContext.setRollBack(true)
                .setRollBackAck(false);
    }
}
```

`ConsumeContext#getRetryCount()` 表示当前消息已经执行过的消费重试次数：原始消息为 `0`，第一次重试消息为 `1`。策略根据该值选择下一段等待时间，业务代码不需要自行累加。

也可以使用以下预置策略：

- `ConsumeRetryPolicy.nativeNack()`：使用 Kafka nack 或 RabbitMQ requeue 进行原生重试，消费者接口默认使用该策略
- `ConsumeRetryPolicy.fixedDelay(5, Duration.ofMinutes(1))`：最多重试 5 次，每次间隔 1 分钟
- `ConsumeRetryPolicy.fixedDelayForever(Duration.ofMinutes(1))`：每分钟重试一次，不限制次数
- `ConsumeRetryPolicy.noRetry()`：不重试，需要消费者显式配置

自定义间隔的数量就是总重试次数，间隔不要求递增。业务失败后，框架先回滚业务事务，再用独立事务将下一轮消息保存到 `TXN_MESSAGE`，通过 `next_dispatch_time` 延迟派发；保存成功后才确认当前 MQ 消息，不会长时间占用消费线程。

当策略返回停止重试时，框架不会在 `TXN_MESSAGE` 创建 `DEAD` 记录，而是直接在独立事务中写入 `TXN_MESSAGE_HISTORY`。死信历史保留 `original_message_id`、`retry_count`、`consumer_code` 和 `last_error`，并通过 `(original_message_id, retry_count)` 唯一键避免 ACK 或 offset 提交失败造成重复归档。死信历史写入成功或已经存在后，框架才确认当前 MQ 消息。

context 或 payload 无法解析时不会盲目重建消息，而是使用 MQ 原生 nack/requeue。当前组件尚未提供死信报警和人工重放接口，业务系统需要监控历史表中的 `message_status = 5`（`MessageStatus.DEAD`）记录并建立相应处置流程。

## 消息链路能力

- 每条消息都有唯一 `id`
- 支持 `parentId` / `rootId`，便于追踪消息派生链路
- 可以通过 `sendWithParent` 基于当前消费上下文继续发送下游消息

示例：

```java
String childId = messagePublishService.sendWithParent(MqType.RABBITMQ, childMessage, parentContext);
```

## 投递语义

当前实现不是 exactly-once。更准确地说，生产到 MQ 的链路可能出现重复投递：

- 消息先落库，再异步派发
- 多实例场景通过数据库抢占减少重复派发
- 但如果消息已发到 MQ、数据库状态还未来得及更新时实例宕机，仍可能重复投递

消费侧在调用业务消费者前，会先向消费记录表写入记录；如果发现相同消息已经消费过，框架会直接确认消息，不再调用业务系统的消费逻辑。因此业务消费逻辑通常不会因为同一条消息的重复到达而重复执行。

首次投递时 `originalMessageId` 与 `messageId` 相同；每轮重试都会生成新的 `messageId`，同时保持 `originalMessageId`
不变。消费记录以 `originalMessageId` 作为主键，所以一次原始消息的所有重试仍共享同一个幂等边界。不同业务系统使用各自独立的消费记录表，
`consumerCode` 只用于审计，不参与幂等键。

需要注意的是，框架消费记录表仍是这套语义的前提。消费记录被清理后，如果非常旧的重复消息再次到达，框架将无法再通过在线消费记录识别它；业务侧若对这种场景敏感，仍应基于业务唯一键做最终保护。

## 业务接入建议

- `messageKey` 建议使用业务唯一键，方便排查和追踪
- `producerCode`、`consumerCode` 建议保持稳定，便于日志和消费记录定位
- `payload` 建议使用结构明确的 JSON 对象
- 首次接入时优先选择单一 MQ，跑通发送、消费、清理链路后再扩展多 MQ

## 本地验证

运行核心测试：

```bash
mvn test
```

运行 demo 测试：

```bash
mvn -pl spring-transactional-mq-demo -am test
```

运行真实 RabbitMQ/Oracle 链路测试：

```bash
mvn -pl spring-transactional-mq-demo -am \
  -Dtest=TransactionalMqRealSendConsumeTest \
  -DrealMqTest=true \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## 当前限制

- 当前仓储实现仅提供 `ez-mybatis`
- 生产到 MQ 的链路不是 exactly-once，MQ 发送成功但数据库状态更新失败时，后续仍可能发生重复投递
- 消费侧通过消费记录表拦截重复消息，但消费记录被清理后无法继续拦截非常旧的重复消息
- README 主要面向业务接入，不覆盖完整源码设计细节
