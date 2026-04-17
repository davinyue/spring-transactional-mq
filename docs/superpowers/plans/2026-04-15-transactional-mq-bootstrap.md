# Spring Transactional MQ Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成事务消息框架首版骨架搭建，落地 Maven 多模块结构、4 张核心表实体、核心 SPI、RabbitMQ 适配入口和 Spring Boot starter 自动装配

**Architecture:** 采用“核心编排 + 存储实现 + MQ 适配 + starter 装配”的分层方案`core` 仅保留领域流程和 SPI，不直接依赖 `EzDao` 与 RabbitMQ；`store-ezmybatis` 提供默认持久化实现，`rabbitmq` 提供首版 MQ 实现，最终由 starter 组装成业务可直接接入的默认能力

**Tech Stack:** JDK 1.8, Maven, Spring Boot 2.7.18, Spring AMQP, ez-mybatis, JUnit 4

---

## File Structure

本次实现涉及的主要文件与职责如下

- Create: `spring-transactional-mq-common/pom.xml`
- Create: `spring-transactional-mq-api/pom.xml`
- Create: `spring-transactional-mq-core/pom.xml`
- Create: `spring-transactional-mq-store-ezmybatis/pom.xml`
- Create: `spring-transactional-mq-rabbitmq/pom.xml`
- Create: `spring-transactional-mq-spring-boot-starter/pom.xml`
- Modify: `pom.xml`

- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/entity/BaseEntity.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/MessageStatus.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/ConsumeStatus.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/SendStatus.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/MqType.java`

- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/model/TransactionalMessage.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/model/ConsumeContext.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/model/SendResult.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/producer/TransactionalMessageSender.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/consumer/TransactionalMessageConsumer.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/serialize/MessagePayloadSerializer.java`

- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/repository/TransactionalMessageRepository.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/repository/ConsumedMessageRepository.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/repository/MessageSendLogRepository.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/mq/MqProducerAdapter.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/MessagePublishService.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/MessageDispatchService.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/ConsumeIdempotentService.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/ConsumedMessageArchiveService.java`

- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/TransactionalMessageEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/ConsumedMessageEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/ConsumedMessageHistoryEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/MessageSendLogEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/EzMybatisTransactionalMessageRepository.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/EzMybatisConsumedMessageRepository.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/EzMybatisMessageSendLogRepository.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/MYSQL.sql`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/ORACLE.sql`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/POSTGRE_SQL.sql`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/SQL_SERVER.sql`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/resources/sql/DM.sql`

- Create: `spring-transactional-mq-rabbitmq/src/main/java/org/rdlinux/transactionalmq/rabbitmq/RabbitMqProducerAdapter.java`
- Create: `spring-transactional-mq-rabbitmq/src/main/java/org/rdlinux/transactionalmq/rabbitmq/RabbitMqConsumerInvoker.java`

- Create: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqProperties.java`
- Create: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfiguration.java`
- Create: `spring-transactional-mq-spring-boot-starter/src/main/resources/META-INF/spring.factories`

- Create: `spring-transactional-mq-store-ezmybatis/src/test/java/...`
- Create: `spring-transactional-mq-core/src/test/java/...`
- Create: `spring-transactional-mq-spring-boot-starter/src/test/java/...`

### Task 1: 重构父工程与模块骨架

**Files:**
- Modify: `pom.xml`
- Create: `spring-transactional-mq-common/pom.xml`
- Create: `spring-transactional-mq-api/pom.xml`
- Create: `spring-transactional-mq-core/pom.xml`
- Create: `spring-transactional-mq-store-ezmybatis/pom.xml`
- Create: `spring-transactional-mq-rabbitmq/pom.xml`
- Create: `spring-transactional-mq-spring-boot-starter/pom.xml`
- Test: `mvn -q -DskipTests package`

- [ ] **Step 1: 先把根 `pom.xml` 改成聚合父工程**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-parent</artifactId>
    <version>0.0.1</version>
    <packaging>pom</packaging>

    <modules>
        <module>spring-transactional-mq-common</module>
        <module>spring-transactional-mq-api</module>
        <module>spring-transactional-mq-core</module>
        <module>spring-transactional-mq-store-ezmybatis</module>
        <module>spring-transactional-mq-rabbitmq</module>
        <module>spring-transactional-mq-spring-boot-starter</module>
    </modules>

    <properties>
        <java.version>1.8</java.version>
        <spring.boot.version>2.7.18</spring.boot.version>
        <ez.mybatis.version>1.0.5.RS</ez.mybatis.version>
    </properties>
</project>
```

- [ ] **Step 2: 为每个子模块建立最小 `pom.xml`**

```xml
<parent>
    <groupId>org.rdlinux</groupId>
    <artifactId>spring-transactional-mq-parent</artifactId>
    <version>0.0.1</version>
</parent>

<artifactId>spring-transactional-mq-core</artifactId>

<dependencies>
    <dependency>
        <groupId>org.rdlinux</groupId>
        <artifactId>spring-transactional-mq-api</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

- [ ] **Step 3: 运行聚合构建验证模块关系**

Run: `mvn -q -DskipTests package`
Expected: SUCCESS，所有模块空骨架可正常打包

- [ ] **Step 4: 提交模块骨架**

```bash
git add pom.xml spring-transactional-mq-*/pom.xml
git commit -m "build: split project into maven modules"
```

### Task 2: 落地公共枚举与基础实体

**Files:**
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/entity/BaseEntity.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/MessageStatus.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/ConsumeStatus.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/SendStatus.java`
- Create: `spring-transactional-mq-common/src/main/java/org/rdlinux/transactionalmq/common/enums/MqType.java`
- Test: `spring-transactional-mq-common/src/test/java/org/rdlinux/transactionalmq/common/entity/BaseEntityTest.java`

- [ ] **Step 1: 编写 `BaseEntity` 最小结构测试**

```java
public class BaseEntityTest {

    @Test
    public void should_store_string_id_and_audit_time() {
        DemoEntity entity = new DemoEntity();
        entity.setId("msg-1");
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        Assert.assertEquals("msg-1", entity.getId());
        Assert.assertEquals(now, entity.getCreateTime());
        Assert.assertEquals(now, entity.getUpdateTime());
    }
}
```

- [ ] **Step 2: 运行测试确认当前失败**

Run: `mvn -q -pl spring-transactional-mq-common -Dtest=BaseEntityTest test`
Expected: FAIL，提示 `BaseEntity` 或 `DemoEntity` 不存在

- [ ] **Step 3: 实现基础实体与枚举**

```java
public abstract class BaseEntity {

    private String id;
    private Date createTime;
    private Date updateTime;
}
```

```java
public enum MessageStatus {
    INIT, SENDING, SUCCESS, RETRYING, DEAD
}
```

- [ ] **Step 4: 运行模块测试**

Run: `mvn -q -pl spring-transactional-mq-common test`
Expected: PASS

- [ ] **Step 5: 提交公共模型**

```bash
git add spring-transactional-mq-common
git commit -m "feat: add common base entity and enums"
```

### Task 3: 定义 API 层模型与业务接口

**Files:**
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/model/TransactionalMessage.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/model/ConsumeContext.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/model/SendResult.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/producer/TransactionalMessageSender.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/consumer/TransactionalMessageConsumer.java`
- Create: `spring-transactional-mq-api/src/main/java/org/rdlinux/transactionalmq/api/serialize/MessagePayloadSerializer.java`
- Test: `spring-transactional-mq-api/src/test/java/org/rdlinux/transactionalmq/api/TransactionalMessageApiTest.java`

- [ ] **Step 1: 先写接口契约测试**

```java
public class TransactionalMessageApiTest {

    @Test
    public void should_keep_consumer_code_and_payload() {
        TransactionalMessage<String> message = new TransactionalMessage<>();
        message.setMessageKey("msg-key");
        message.setPayload("hello");
        Assert.assertEquals("msg-key", message.getMessageKey());
        Assert.assertEquals("hello", message.getPayload());
    }
}
```

- [ ] **Step 2: 执行测试确认接口未落地**

Run: `mvn -q -pl spring-transactional-mq-api -Dtest=TransactionalMessageApiTest test`
Expected: FAIL，提示 API 模型不存在

- [ ] **Step 3: 实现最小 API**

```java
public interface TransactionalMessageSender {

    <T> SendResult send(TransactionalMessage<T> message);
}
```

```java
public interface TransactionalMessageConsumer<T> {

    String consumerCode();

    void consume(ConsumeContext context, T payload);
}
```

- [ ] **Step 4: 运行 API 模块测试**

Run: `mvn -q -pl spring-transactional-mq-api test`
Expected: PASS

- [ ] **Step 5: 提交 API 层**

```bash
git add spring-transactional-mq-api
git commit -m "feat: add transactional mq api contracts"
```

### Task 4: 定义 Core 层 SPI 与领域服务骨架

**Files:**
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/repository/TransactionalMessageRepository.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/repository/ConsumedMessageRepository.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/repository/MessageSendLogRepository.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/mq/MqProducerAdapter.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/MessagePublishService.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/MessageDispatchService.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/ConsumeIdempotentService.java`
- Create: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/ConsumedMessageArchiveService.java`
- Test: `spring-transactional-mq-core/src/test/java/org/rdlinux/transactionalmq/core/service/MessagePublishServiceTest.java`

- [ ] **Step 1: 编写发布服务失败测试**

```java
public class MessagePublishServiceTest {

    @Test
    public void should_save_message_before_returning_send_result() {
        InMemoryTransactionalMessageRepository repository = new InMemoryTransactionalMessageRepository();
        MessagePublishService service = new MessagePublishService(repository);
        SendResult result = service.publish(buildMessage());
        Assert.assertTrue(result.isAccepted());
        Assert.assertEquals(1, repository.count());
    }
}
```

- [ ] **Step 2: 运行测试确认 Core 服务不存在**

Run: `mvn -q -pl spring-transactional-mq-core -Dtest=MessagePublishServiceTest test`
Expected: FAIL

- [ ] **Step 3: 落地最小 SPI 与服务骨架**

```java
public interface TransactionalMessageRepository {

    void save(TransactionalMessageRecord record);

    List<TransactionalMessageRecord> findDispatchableMessages(Date now, int limit);
}
```

```java
public class MessagePublishService {

    private final TransactionalMessageRepository repository;

    public <T> SendResult publish(TransactionalMessage<T> message) {
        repository.save(TransactionalMessageRecord.from(message));
        return SendResult.accepted(message.getMessageKey());
    }
}
```

- [ ] **Step 4: 补充派发、幂等、归档服务空实现与接口签名**

```java
public class ConsumeIdempotentService {

    public boolean shouldConsume(String id, String consumerCode) {
        return true;
    }
}
```

- [ ] **Step 5: 运行 Core 模块测试**

Run: `mvn -q -pl spring-transactional-mq-core test`
Expected: PASS

- [ ] **Step 6: 提交 Core 骨架**

```bash
git add spring-transactional-mq-core
git commit -m "feat: add core repositories and domain services"
```

### Task 5: 建立 4 张核心表实体与 ez-mybatis 仓储实现

**Files:**
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/TransactionalMessageEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/ConsumedMessageEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/ConsumedMessageHistoryEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/MessageSendLogEntity.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/EzMybatisTransactionalMessageRepository.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/EzMybatisConsumedMessageRepository.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/main/java/org/rdlinux/transactionalmq/store/ezmybatis/repository/EzMybatisMessageSendLogRepository.java`
- Create: `spring-transactional-mq-store-ezmybatis/src/test/java/org/rdlinux/transactionalmq/store/ezmybatis/entity/TransactionalMessageEntityTest.java`
- Test: `mvn -q -pl spring-transactional-mq-store-ezmybatis test`

- [ ] **Step 1: 为事务消息实体写字段测试**

```java
public class TransactionalMessageEntityTest {

    @Test
    public void should_extend_base_entity_and_keep_dispatch_fields() {
        TransactionalMessageEntity entity = new TransactionalMessageEntity();
        entity.setId("id-1");
        entity.setMessageKey("msg-key");
        entity.setRetryCount(0);
        Assert.assertEquals("id-1", entity.getId());
        Assert.assertEquals("msg-key", entity.getMessageKey());
        Assert.assertEquals(Integer.valueOf(0), entity.getRetryCount());
    }
}
```

- [ ] **Step 2: 运行测试确认实体不存在**

Run: `mvn -q -pl spring-transactional-mq-store-ezmybatis -Dtest=TransactionalMessageEntityTest test`
Expected: FAIL

- [ ] **Step 3: 实现 4 张表实体**

```java
public class TransactionalMessageEntity extends BaseEntity {

    private String messageKey;
    private String producerCode;
    private String mqType;
    private String destinationType;
    private String destinationName;
    private String routingKey;
    private String messageBody;
    private String headersJson;
    private String bizKey;
    private String messageStatus;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Date nextRetryTime;
}
```

```java
public class ConsumedMessageEntity extends BaseEntity {

    private String id;
    private String consumerCode;
    private String consumeStatus;
    private Date consumeTime;
}
```

- [ ] **Step 4: 基于 `EzDao` 实现仓储**

```java
public class EzMybatisTransactionalMessageRepository implements TransactionalMessageRepository {

    private final EzDao ezDao;

    @Override
    public void save(TransactionalMessageRecord record) {
        ezDao.insert(TransactionalMessageEntityConverter.toEntity(record));
    }
}
```

- [ ] **Step 5: 增加消费记录归档仓储方法**

```java
public interface ConsumedMessageRepository {

    boolean exists(String id, String consumerCode);

    void save(ConsumedMessageRecord record);

    int archiveBefore(Date threshold, int limit);
}
```

- [ ] **Step 6: 补齐多数据库 SQL 模板**

```sql
CREATE TABLE txn_message (
  id VARCHAR(64) PRIMARY KEY,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  message_key VARCHAR(128) NOT NULL,
  producer_code VARCHAR(64) NOT NULL
);
```

- [ ] **Step 7: 运行存储模块测试**

Run: `mvn -q -pl spring-transactional-mq-store-ezmybatis test`
Expected: PASS

- [ ] **Step 8: 提交存储模块**

```bash
git add spring-transactional-mq-store-ezmybatis
git commit -m "feat: add ez-mybatis repositories and entities"
```

### Task 6: 实现 RabbitMQ 首版发送适配

**Files:**
- Create: `spring-transactional-mq-rabbitmq/src/main/java/org/rdlinux/transactionalmq/rabbitmq/RabbitMqProducerAdapter.java`
- Create: `spring-transactional-mq-rabbitmq/src/main/java/org/rdlinux/transactionalmq/rabbitmq/RabbitMqConsumerInvoker.java`
- Create: `spring-transactional-mq-rabbitmq/src/test/java/org/rdlinux/transactionalmq/rabbitmq/RabbitMqProducerAdapterTest.java`

- [ ] **Step 1: 先为 producer adapter 写单测**

```java
public class RabbitMqProducerAdapterTest {

    @Test
    public void should_convert_record_to_rabbit_template_call() {
        RabbitTemplate template = Mockito.mock(RabbitTemplate.class);
        RabbitMqProducerAdapter adapter = new RabbitMqProducerAdapter(template);
        adapter.send(buildRecord());
        Mockito.verify(template).convertAndSend("order.exchange", "order.created", "{\"id\":1}");
    }
}
```

- [ ] **Step 2: 运行测试确认适配器不存在**

Run: `mvn -q -pl spring-transactional-mq-rabbitmq -Dtest=RabbitMqProducerAdapterTest test`
Expected: FAIL

- [ ] **Step 3: 实现 `MqProducerAdapter` 的 RabbitMQ 版本**

```java
public class RabbitMqProducerAdapter implements MqProducerAdapter {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(DispatchMessage command) {
        rabbitTemplate.convertAndSend(command.getDestinationName(), command.getRoutingKey(), command.getMessageBody());
    }
}
```

- [ ] **Step 4: 添加消费者调用桥接骨架**

```java
public class RabbitMqConsumerInvoker {

    public <T> void invoke(TransactionalMessageConsumer<T> consumer, ConsumeContext context, T payload) {
        consumer.consume(context, payload);
    }
}
```

- [ ] **Step 5: 运行 RabbitMQ 模块测试**

Run: `mvn -q -pl spring-transactional-mq-rabbitmq test`
Expected: PASS

- [ ] **Step 6: 提交 RabbitMQ 适配**

```bash
git add spring-transactional-mq-rabbitmq
git commit -m "feat: add rabbitmq producer adapter"
```

### Task 7: 实现 starter 自动装配与配置绑定

**Files:**
- Create: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqProperties.java`
- Create: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfiguration.java`
- Create: `spring-transactional-mq-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- Create: `spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java`

- [ ] **Step 1: 为 starter 自动装配写上下文测试**

```java
public class TransactionalMqAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TransactionalMqAutoConfiguration.class));

    @Test
    public void should_register_message_publish_service() {
        contextRunner.run(context -> Assert.assertNotNull(context.getBean(MessagePublishService.class)));
    }
}
```

- [ ] **Step 2: 运行测试确认自动装配尚未实现**

Run: `mvn -q -pl spring-transactional-mq-spring-boot-starter -Dtest=TransactionalMqAutoConfigurationTest test`
Expected: FAIL

- [ ] **Step 3: 实现配置类与自动装配**

```java
@ConfigurationProperties(prefix = "transactional.mq")
public class TransactionalMqProperties {

    private boolean enabled = true;
    private int dispatchBatchSize = 100;
    private int consumeRecordRetentionDays = 7;
}
```

```java
@Configuration
@EnableConfigurationProperties(TransactionalMqProperties.class)
public class TransactionalMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessagePublishService messagePublishService(TransactionalMessageRepository repository) {
        return new MessagePublishService(repository);
    }
}
```

- [ ] **Step 4: 在 `spring.factories` 注册自动配置**

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.rdlinux.transactionalmq.starter.config.TransactionalMqAutoConfiguration
```

- [ ] **Step 5: 运行 starter 模块测试**

Run: `mvn -q -pl spring-transactional-mq-spring-boot-starter test`
Expected: PASS

- [ ] **Step 6: 提交 starter 自动装配**

```bash
git add spring-transactional-mq-spring-boot-starter
git commit -m "feat: add spring boot starter auto configuration"
```

### Task 8: 打通最小主链路与回归验证

**Files:**
- Modify: `spring-transactional-mq-core/src/main/java/...`
- Modify: `spring-transactional-mq-store-ezmybatis/src/main/java/...`
- Modify: `spring-transactional-mq-rabbitmq/src/main/java/...`
- Modify: `spring-transactional-mq-spring-boot-starter/src/main/java/...`
- Test: `mvn -q test`

- [ ] **Step 1: 增加一条最小集成测试**

```java
public class TransactionalMqBootstrapIntegrationTest {

    @Test
    public void should_accept_message_and_persist_before_dispatch() {
        SendResult result = sender.send(buildMessage());
        Assert.assertTrue(result.isAccepted());
        Assert.assertEquals(1, transactionalMessageRepository.countPending());
    }
}
```

- [ ] **Step 2: 运行集成测试确认仍有缺口**

Run: `mvn -q test`
Expected: 至少有一个集成场景失败，暴露 wiring 缺口

- [ ] **Step 3: 补齐 wiring 和缺失依赖**

```java
@Bean
@ConditionalOnMissingBean
public RabbitMqProducerAdapter mqProducerAdapter(RabbitTemplate rabbitTemplate) {
    return new RabbitMqProducerAdapter(rabbitTemplate);
}
```

- [ ] **Step 4: 执行全量最小验证**

Run: `mvn -q test`
Expected: PASS

- [ ] **Step 5: 执行聚合打包**

Run: `mvn -q package`
Expected: SUCCESS

- [ ] **Step 6: 提交首版骨架**

```bash
git add .
git commit -m "feat: bootstrap transactional mq starter skeleton"
```

## Self-Review

### Spec coverage

- Maven 模块拆分：由 Task 1 覆盖
- `BaseEntity` 与审计字段：由 Task 2、Task 5 覆盖
- 4 张核心表：由 Task 5 覆盖
- Core SPI：由 Task 4 覆盖
- RabbitMQ 首版支持：由 Task 6 覆盖
- Spring Boot starter 主接入方式：由 Task 7、Task 8 覆盖

未发现 spec 要求缺口

### Placeholder scan

- 未使用 `TODO`、`TBD`、`implement later`
- 所有任务均给出目标文件、命令和最小代码骨架

### Type consistency

- `BaseEntity.id` 统一为 `String`
- 审计字段统一使用 `Date`
- `consumeRecordRetentionDays` 与“7 天归档”规则一致
