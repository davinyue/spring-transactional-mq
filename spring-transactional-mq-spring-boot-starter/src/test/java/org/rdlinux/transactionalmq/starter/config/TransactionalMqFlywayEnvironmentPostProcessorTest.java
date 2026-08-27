package org.rdlinux.transactionalmq.starter.config;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.Collections;

/**
 * {@link TransactionalMqFlywayEnvironmentPostProcessor} 测试。
 */
public class TransactionalMqFlywayEnvironmentPostProcessorTest {

    /**
     * 环境后置处理器应由 Spring Boot 从 spring.factories 加载。
     */
    @Test
    public void should_be_listed_in_spring_factories() {
        Assert.assertTrue(SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class,
                this.getClass().getClassLoader()).contains(TransactionalMqFlywayEnvironmentPostProcessor.class.getName()));
    }

    /**
     * 未配置业务 Flyway 时，应关闭 Spring Boot 默认 Flyway 自动配置。
     */
    @Test
    public void should_disable_boot_flyway_by_default() {
        StandardEnvironment environment = new StandardEnvironment();

        new TransactionalMqFlywayEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        Assert.assertEquals("false", environment.getProperty("spring.flyway.enabled"));
    }

    /**
     * 业务工程显式配置 Flyway 时，应保留其配置。
     */
    @Test
    public void should_preserve_business_flyway_setting() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("businessFlyway",
                Collections.<String, Object>singletonMap("spring.flyway.enabled", "true")));

        new TransactionalMqFlywayEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        Assert.assertEquals("true", environment.getProperty("spring.flyway.enabled"));
    }
}
