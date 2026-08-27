package org.rdlinux.transactionalmq.starter.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;
import java.util.Map;

/**
 * 为未使用业务 Flyway 的应用关闭 Spring Boot 默认 Flyway 自动配置。
 *
 * <p>事务消息迁移由 {@link TransactionalMqFlywayInitializer} 独立执行；业务工程显式配置
 * {@code spring.flyway.enabled} 时保留其自身的 Flyway 自动配置。</p>
 */
public class TransactionalMqFlywayEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * Spring Boot Flyway 开关配置项。
     */
    private static final String FLYWAY_ENABLED_PROPERTY = "spring.flyway.enabled";

    /**
     * starter 提供的默认配置源名称。
     */
    private static final String PROPERTY_SOURCE_NAME = "transactionalMqFlywayDefaults";

    /**
     * 在业务工程未显式设置 Flyway 开关时，关闭 Spring Boot 默认 Flyway 自动配置。
     *
     * @param environment 当前应用环境
     * @param application 当前 Spring Boot 应用
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(FLYWAY_ENABLED_PROPERTY)) {
            return;
        }
        Map<String, Object> properties = Collections.singletonMap(FLYWAY_ENABLED_PROPERTY, false);
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    /**
     * 在应用配置加载后添加默认值，确保业务工程显式配置具有更高优先级。
     *
     * @return 后置处理顺序
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
