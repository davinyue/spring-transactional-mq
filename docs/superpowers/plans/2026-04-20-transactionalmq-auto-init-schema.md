# Transactional MQ Auto Init Schema Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add default-enabled startup schema initialization for `spring-transactional-mq` that detects the ez-mybatis database type, selects the matching SQL script, executes it on startup, and swallows any initialization exception without blocking application startup.

**Architecture:** Keep the feature in the starter layer by adding a dedicated schema initializer bean plus one starter property switch. Reuse the existing SQL files under `spring-transactional-mq-store-ezmybatis`, resolve `DbType` from ez-mybatis configuration, and keep failure handling intentionally silent to match the confirmed requirement.

**Tech Stack:** Spring Boot auto-configuration, `ConfigurationProperties`, JUnit4, `ApplicationContextRunner`, Spring JDBC `ResourceDatabasePopulator`, ez-mybatis integration

---

### Task 1: Lock Down Starter Property Behavior

**Files:**
- Modify: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqProperties.java`
- Modify: `spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java`

- [ ] **Step 1: Write the failing property assertion test**

Add a new test to `TransactionalMqAutoConfigurationTest` that starts the context with `EzDao` and `RabbitTemplate`, retrieves `TransactionalMqProperties`, and asserts the new `autoInitSchema` property defaults to `true`.

```java
    @Test
    public void should_enable_auto_init_schema_by_default() {
        this.contextRunner
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
                    TransactionalMqProperties properties = context.getBean(TransactionalMqProperties.class);
                    Assert.assertTrue(properties.isAutoInitSchema());
                });
    }
```

- [ ] **Step 2: Run the property test to verify it fails**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter -am \
  -Dtest=TransactionalMqAutoConfigurationTest#should_enable_auto_init_schema_by_default \
  test
```

Expected: FAIL because `TransactionalMqProperties` does not yet expose `autoInitSchema`.

- [ ] **Step 3: Add the minimal property implementation**

Update `TransactionalMqProperties.java` to add the new field with default value `true`.

```java
    /**
     * 是否自动执行事务消息建表
     */
    private boolean autoInitSchema = true;
```

Keep Lombok-managed getter/setter behavior consistent with the existing class.

- [ ] **Step 4: Run the property test to verify it passes**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter -am \
  -Dtest=TransactionalMqAutoConfigurationTest#should_enable_auto_init_schema_by_default \
  test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add \
  spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqProperties.java \
  spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java
git commit -m "test: add auto init schema starter property"
```

### Task 2: Add Schema Initializer and Script Resolution

**Files:**
- Create: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqSchemaInitializer.java`
- Modify: `spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java`

- [ ] **Step 1: Write the failing script resolution tests**

Add focused unit tests for a new `TransactionalMqSchemaInitializer` type that verify:

1. `MYSQL` resolves to `classpath:sql/MYSQL.sql`
2. `ORACLE` resolves to `classpath:sql/ORACLE.sql`
3. `DM` resolves to `classpath:sql/DM.sql`
4. `POSTGRE_SQL` resolves to `classpath:sql/POSTGRE_SQL.sql`
5. `SQL_SERVER` resolves to `classpath:sql/SQL_SERVER.sql`

Use the real `DbType` enum from ez-mybatis once the exact package is located during implementation.

Example test shape:

```java
    @Test
    public void resolveScriptLocationShouldMatchMysql() {
        Assert.assertEquals("classpath:sql/MYSQL.sql",
                TransactionalMqSchemaInitializer.resolveScriptLocation(DbType.MYSQL));
    }
```

- [ ] **Step 2: Run the resolution tests to verify they fail**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter -am \
  -Dtest=TransactionalMqAutoConfigurationTest#resolveScriptLocationShouldMatchMysql \
  test
```

Expected: FAIL because `TransactionalMqSchemaInitializer` does not yet exist.

- [ ] **Step 3: Create the minimal initializer with script mapping**

Create `TransactionalMqSchemaInitializer.java` with these responsibilities:

- hold `DataSource`
- resolve `DbType` from ez-mybatis `Configuration`
- map `DbType` to classpath SQL script
- execute script with `ResourceDatabasePopulator`
- swallow any exception

Minimum structure:

```java
public class TransactionalMqSchemaInitializer implements InitializingBean {

    private final DataSource dataSource;
    private final Object ezMybatisConfiguration;

    public TransactionalMqSchemaInitializer(DataSource dataSource, Object ezMybatisConfiguration) {
        this.dataSource = dataSource;
        this.ezMybatisConfiguration = ezMybatisConfiguration;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            DbType dbType = this.resolveDbType();
            String scriptLocation = resolveScriptLocation(dbType);
            if (scriptLocation == null) {
                return;
            }
            ResourceDatabasePopulator populator =
                    new ResourceDatabasePopulator(new ClassPathResource(scriptLocation.replace("classpath:", "")));
            DatabasePopulatorUtils.execute(populator, this.dataSource);
        } catch (Exception ex) {
            // swallow exception by requirement
        }
    }

    static String resolveScriptLocation(DbType dbType) {
        // map db type here
    }
}
```

During implementation, replace `Object ezMybatisConfiguration` with the exact ez-mybatis `Configuration` type and call the exact `EzMybatisContent.getDbType(configuration)` API.

- [ ] **Step 4: Run the resolution tests to verify they pass**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter -am \
  -Dtest=TransactionalMqAutoConfigurationTest \
  test
```

Expected: the new script resolution assertions PASS.

- [ ] **Step 5: Commit**

```bash
git add \
  spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqSchemaInitializer.java \
  spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java
git commit -m "feat: add schema initializer script resolution"
```

### Task 3: Wire Schema Initializer Into Auto-Configuration

**Files:**
- Modify: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfiguration.java`
- Modify: `spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java`

- [ ] **Step 1: Write failing auto-configuration tests**

Add tests that verify:

1. With default config and required dependencies present, the context contains `transactionalMqSchemaInitializer`
2. With `transactionalmq.auto-init-schema=false`, the context does not contain the initializer bean
3. If initializer execution throws, the context still starts successfully

Suggested test shapes:

```java
    @Test
    public void should_register_schema_initializer_when_auto_init_schema_enabled() {
        this.contextRunner
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .withBean(javax.sql.DataSource.class, () -> mock(javax.sql.DataSource.class))
                .run(context -> Assert.assertTrue(context.containsBean("transactionalMqSchemaInitializer")));
    }

    @Test
    public void should_not_register_schema_initializer_when_auto_init_schema_disabled() {
        this.contextRunner
                .withPropertyValues("transactionalmq.auto-init-schema=false")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .withBean(javax.sql.DataSource.class, () -> mock(javax.sql.DataSource.class))
                .run(context -> Assert.assertFalse(context.containsBean("transactionalMqSchemaInitializer")));
    }
```

- [ ] **Step 2: Run the auto-configuration tests to verify they fail**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter -am \
  -Dtest=TransactionalMqAutoConfigurationTest \
  test
```

Expected: FAIL because the initializer bean is not yet registered.

- [ ] **Step 3: Add minimal auto-configuration wiring**

Register the initializer bean in `TransactionalMqAutoConfiguration.java` with conditions:

- `transactionalmq.enabled=true`
- `transactionalmq.auto-init-schema=true`
- `EzDao` present
- `DataSource` present
- exact ez-mybatis configuration bean present
- `TransactionalMqSchemaInitializer` missing

Example structure:

```java
    @Bean
    @ConditionalOnClass(EzDao.class)
    @ConditionalOnBean({EzDao.class, javax.sql.DataSource.class, ExactEzMybatisConfigurationType.class})
    @ConditionalOnProperty(prefix = "transactionalmq", name = "auto-init-schema",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(TransactionalMqSchemaInitializer.class)
    public TransactionalMqSchemaInitializer transactionalMqSchemaInitializer(
            javax.sql.DataSource dataSource,
            ExactEzMybatisConfigurationType configuration) {
        return new TransactionalMqSchemaInitializer(dataSource, configuration);
    }
```

Replace `ExactEzMybatisConfigurationType` with the real ez-mybatis configuration bean type discovered during implementation.

- [ ] **Step 4: Run the auto-configuration tests to verify they pass**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter -am \
  -Dtest=TransactionalMqAutoConfigurationTest \
  test
```

Expected: PASS, including the disabled-property case and swallowed-exception case.

- [ ] **Step 5: Commit**

```bash
git add \
  spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfiguration.java \
  spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java
git commit -m "feat: auto configure transactional mq schema initializer"
```

### Task 4: Update Documentation and Requirement Record

**Files:**
- Modify: `README.md`
- Create: `story/2026-04-20-自动建表初始化.md`

- [ ] **Step 1: Write the failing documentation checklist**

Before editing, make a checklist in the story file that requires README to mention:

- auto-init schema is default-enabled
- property name is `transactionalmq.auto-init-schema`
- schema selection is based on ez-mybatis database type
- startup is not blocked if schema initialization fails

Checklist body:

```markdown
## 验收核对

- [ ] README 说明自动建表默认开启
- [ ] README 写明配置项 `transactionalmq.auto-init-schema`
- [ ] README 写明按 ez-mybatis 数据库类型选择脚本
- [ ] README 写明建表异常不会影响启动
```

- [ ] **Step 2: Update the story record**

Create `story/2026-04-20-自动建表初始化.md` using the repository story template and mark analysis items complete while leaving implementation/verification tasks to be updated after code is done.

- [ ] **Step 3: Update README minimally**

Add or update the relevant README sections so they explicitly document:

- default schema auto-init behavior
- opt-out property
- supported DB script mapping at a high level
- swallowed startup failure behavior

- [ ] **Step 4: Verify documentation changes**

Run:

```bash
rg -n "auto-init-schema|自动建表|不会影响启动|ez-mybatis" README.md story/2026-04-20-自动建表初始化.md
```

Expected: output includes all newly added documentation lines.

- [ ] **Step 5: Commit**

```bash
git add README.md story/2026-04-20-自动建表初始化.md
git commit -m "docs: describe transactional mq auto schema init"
```

### Task 5: Final Verification

**Files:**
- Modify: `spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java`
- Modify: `README.md`
- Modify: `story/2026-04-20-自动建表初始化.md`

- [ ] **Step 1: Run the focused starter test suite**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter -am \
  -Dtest=TransactionalMqAutoConfigurationTest \
  test
```

Expected: PASS.

- [ ] **Step 2: Run the broader related module verification**

Run:

```bash
mvn -pl spring-transactional-mq-spring-boot-starter,spring-transactional-mq-store-ezmybatis -am test
```

Expected: PASS, or if unrelated legacy failures appear, capture them explicitly in the story file before claiming completion.

- [ ] **Step 3: Update story progress and risks**

Mark all completed tasks in `story/2026-04-20-自动建表初始化.md`, record the exact verification commands executed, and note that schema initialization intentionally swallows exceptions by product decision.

- [ ] **Step 4: Commit**

```bash
git add \
  spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java \
  README.md \
  story/2026-04-20-自动建表初始化.md
git commit -m "test: verify transactional mq auto schema init"
```
