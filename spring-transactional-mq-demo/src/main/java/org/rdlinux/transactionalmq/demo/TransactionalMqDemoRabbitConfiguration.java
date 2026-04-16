package org.rdlinux.transactionalmq.demo;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * demo RabbitMQ 资源配置。
 */
@Configuration
public class TransactionalMqDemoRabbitConfiguration {

    /**
     * 声明默认测试队列。默认 exchange 发送时，destination 直接使用该队列名即可。
     *
     * @param properties demo 配置
     * @return 测试队列
     */
    @Bean
    @ConditionalOnProperty(prefix = "demo.transactional-mq", name = "auto-declare-queue",
            havingValue = "true", matchIfMissing = true)
    public Queue transactionalMqDemoQueue(TransactionalMqDemoProperties properties) {
        return new Queue(properties.getQueueName(), true);
    }
}
