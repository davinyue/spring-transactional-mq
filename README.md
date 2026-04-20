# spring-transactional-mq

`spring-transactional-mq` 是一个面向 Spring Boot 的事务消息组件。业务侧先把消息写入数据库，再由后台派发线程异步投递到 MQ；消费侧在执行业务逻辑前记录消费记录，通过数据库唯一约束实现幂等控制。

它更适合“本地事务成功后，再可靠地把事件投递到 MQ”这类场景，例如订单创建、支付完成、库存变更后的异步通知。

## 适用场景

- 业务数据和消息投递需要先后有序，要求先落本地事务，再异步发 MQ
- 可以接受“至少一次投递”
- 消费端能够做幂等处理
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

| 模块 | 说明 |
| --- | --- |
| `spring-transactional-mq-common` | 公共枚举、基础实体、ID 生成器 |
| `spring-transactional-mq-api` | 对业务暴露的消息模型、生产/消费接口、序列化接口 |
| `spring-transactional-mq-core` | 消息发布、消息派发、生产路由、消费幂等、清理服务 |
| `spring-transactional-mq-store-ezmybatis` | 基于 `ez-mybatis` 的仓储实现和建表 SQL |
| `spring-transactional-mq-rabbitmq` | RabbitMQ 生产者适配器、消费者自动注册与监听处理 |
| `spring-transactional-mq-kafka` | Kafka 生产者适配器、消费者自动注册与监听处理 |
| `spring-transactional-mq-spring-boot-starter` | Spring Boot 自动装配入口 |
| `spring-transactional-mq-demo` | demo 工程与真实环境链路测试示例 |

## 快速接入

### 1. 引入依赖

业务项目至少需要引入 starter 和你要使用的 MQ 模块。

RabbitMQ 场景：

```xml
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-rabbitmq</artifactId>
    <version>0.0.1</version>
</dependency>
```

Kafka 场景：

```xml
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-kafka</artifactId>
    <version>0.0.1</version>
</dependency>
```

说明：

- `spring-transactional-mq-spring-boot-starter` 中对 RabbitMQ/Kafka 模块使用的是 `optional` 依赖，业务项目需要显式引入
- 如果要发 RabbitMQ，需要正常配置 `spring.rabbitmq.*`
- 如果要发 Kafka，需要正常配置 `spring.kafka.*`
- 当前仓储实现依赖 `EzDao`，因此业务项目需要正常接入 `ez-mybatis`

### 2. 初始化数据库表

建表 SQL 位于：

- `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/MYSQL.sql`
- `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/ORACLE.sql`
- `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/DM.sql`
- `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/POSTGRE_SQL.sql`
- `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/SQL_SERVER.sql`

核心表如下：

| 表名 | 说明 |
| --- | --- |
| `TXN_MESSAGE` | 事务消息主表，保存待派发消息 |
| `TXN_MESSAGE_HISTORY` | 成功消息历史表 |
| `TXN_MESSAGE_SEND_LOG` | 发送日志表 |
| `TXN_CONSUMED_MESSAGE` | 在线消费去重表 |
| `TXN_CONSUMED_MESSAGE_HISTORY` | 消费记录历史表 |

### 3. 配置参数

最小配置示例：

```yaml
transactionalmq:
  enabled: true
  auto-init-schema: true
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
- 有 `EzDao`、`DataSource` 和 `SqlSessionFactory` 时，默认尝试自动初始化事务消息表结构
- 有 RabbitMQ 相关类和连接工厂时装配 RabbitMQ 发送与消费
- 有 Kafka 相关类和消费者工厂时装配 Kafka 发送与消费

## 自动建表

默认情况下，starter 会在启动时尝试自动初始化事务消息相关表结构：

- 默认配置：`transactionalmq.auto-init-schema=true`
- 数据库类型来源：`EzMybatisContent.getDbType(Configuration)`
- 会根据 ez-mybatis 的数据库类型自动选择对应 SQL 脚本

当前映射关系如下：

- `MYSQL -> sql/MYSQL.sql`
- `ORACLE -> sql/ORACLE.sql`
- `DM -> sql/DM.sql`
- `POSTGRE_SQL -> sql/POSTGRE_SQL.sql`
- `SQL_SERVER -> sql/SQL_SERVER.sql`

如果你不希望框架在启动时尝试建表，可以显式关闭：

```yaml
transactionalmq:
  auto-init-schema: false
```

说明：

- 自动建表是“尽力而为”策略
- 如果脚本执行失败，框架会直接吞掉异常，不影响应用启动
- 建表失败后的处理由业务方自行负责

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
    public QueueMsgHandleRet consume(ConsumeContext context, Map<String, Object> payload) {
        // 处理业务逻辑
        return QueueMsgHandleRet.DEFAULT();
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
    public QueueMsgHandleRet consume(ConsumeContext context, String payload) {
        return QueueMsgHandleRet.DEFAULT();
    }
}
```

说明：

- `getQueueName()` 表示 RabbitMQ 的队列名，或 Kafka 的 topic 名
- `getSupportMqType()` 用于声明该消费者属于哪个 MQ
- 消费前框架会先记录消费记录，重复消息会被幂等逻辑拦截
- 消费逻辑运行在事务中，返回 `QueueMsgHandleRet` 可控制提交或回滚

## 消息链路能力

- 每条消息都有唯一 `id`
- 支持 `parentId` / `rootId`，便于追踪消息派生链路
- 可以通过 `sendWithParent` 基于当前消费上下文继续发送下游消息

示例：

```java
String childId = messagePublishService.sendWithParent(MqType.RABBITMQ, childMessage, parentContext);
```

## 投递语义

当前实现是“至少一次投递”：

- 消息先落库，再异步派发
- 多实例场景通过数据库抢占减少重复派发
- 但如果消息已发到 MQ、数据库状态还未来得及更新时实例宕机，仍可能重复投递

所以消费端必须保持幂等，这不是可选项，而是默认前提。

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
- 事务消息语义为至少一次投递，不是 exactly-once
- MQ 发送成功但数据库状态更新失败时，后续仍可能发生重复投递
- README 主要面向业务接入，不覆盖完整源码设计细节
