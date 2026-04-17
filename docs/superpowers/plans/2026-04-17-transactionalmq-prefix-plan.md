# Transactionalmq Prefix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将事务消息组件配置前缀从 `transactional.mq` 收敛为一级前缀 `transactionalmq`，并保持现有配置语义不变

**Architecture:** 这次调整只改配置绑定入口，不改业务逻辑starter 负责的 `@ConfigurationProperties`、`@ConditionalOnProperty` 和 scheduler 的 cron 占位符统一切换到 `transactionalmq`，测试和 demo 示例同步更新

**Tech Stack:** Spring Boot auto-configuration、`@ConfigurationProperties`、JUnit4、Maven

---

### Task 1: 锁定新的配置入口

**Files:**
- Modify: `spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfigurationTest.java`
- Modify: `spring-transactional-mq-spring-boot-starter/src/test/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqMainChainIntegrationTest.java`

- [ ] Step 1: 先把 starter 测试中的属性前缀改成 `transactionalmq`
- [ ] Step 2: 运行相关测试，确认因生产代码仍使用旧前缀而失败
- [ ] Step 3: 记录失败点，确保是预期中的配置绑定或自动装配失败

### Task 2: 切换 starter 配置绑定

**Files:**
- Modify: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqProperties.java`
- Modify: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/TransactionalMqAutoConfiguration.java`
- Modify: `spring-transactional-mq-spring-boot-starter/src/main/java/org/rdlinux/transactionalmq/starter/config/ConsumedMessageCleanupScheduler.java`
- Modify: `spring-transactional-mq-core/src/main/java/org/rdlinux/transactionalmq/core/service/TransactionalMessageCleanupScheduler.java`

- [ ] Step 1: 将 `@ConfigurationProperties` 前缀改为 `transactionalmq`
- [ ] Step 2: 将 `@ConditionalOnProperty` 和两个 cron 占位符改为 `transactionalmq.*`
- [ ] Step 3: 运行 starter 相关测试，确认切换后恢复通过

### Task 3: 同步示例与记录

**Files:**
- Modify: `spring-transactional-mq-demo/src/main/resources/application.yml`
- Modify: `spring-transactional-mq-demo/src/main/resources/application-real.yml`
- Add: `story/2026-04-17-配置前缀一级化调整.md`

- [ ] Step 1: 补充需求记录，说明前缀从 `transactional.mq` 改为 `transactionalmq`
- [ ] Step 2: 同步 demo 示例配置中的前缀
- [ ] Step 3: 运行最小必要验证并记录结果
