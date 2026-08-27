package org.rdlinux.transactionalmq.demo;

import org.rdlinux.transactionalmq.api.consumer.ConsumeHandleContext;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * demo 事务消息消费者示例
 */
@Component
@ConditionalOnProperty(prefix = "demo.transactional-mq", name = "enable-demo-consumer", havingValue = "true",
        matchIfMissing = true)
public class TransactionalMqDemoConsumer implements TransactionalMessageConsumer<Map<?, ?>> {

    /**
     * 日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalMqDemoConsumer.class);

    /**
     * Demo 消费者配置
     */
    private final TransactionalMqDemoProperties properties;

    /**
     * 构造 Demo 消费者
     *
     * @param properties Demo 配置
     */
    public TransactionalMqDemoConsumer(TransactionalMqDemoProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getQueueName() {
        return this.properties.getQueueName();
    }

    @Override
    public int getMinConcurrency() {
        return this.properties.getConsumerMinConcurrency();
    }

    @Override
    public int getMaxConcurrency() {
        return this.properties.getConsumerMaxConcurrency();
    }

    @Override
    public MqType getSupportMqType() {
        return MqType.RABBITMQ;
    }

    @Override
    public String consumerCode() {
        return "spring-transactional-mq-demo-consumer";
    }

    @Override
    public void consume(ConsumeContext context, ConsumeHandleContext handleContext, Map<?, ?> payload) {
        LOGGER.info("demo consumer received message, id={}, messageKey={}, payload={}",
                context.getId(), context.getMessageKey(), payload);
    }
}
