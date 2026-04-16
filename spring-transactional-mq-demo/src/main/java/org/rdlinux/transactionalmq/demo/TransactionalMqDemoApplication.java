package org.rdlinux.transactionalmq.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 事务消息 Spring Boot Demo 启动类。
 */
@SpringBootApplication
@EnableConfigurationProperties(TransactionalMqDemoProperties.class)
public class TransactionalMqDemoApplication {

    /**
     * 启动 demo 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TransactionalMqDemoApplication.class, args);
    }
}
