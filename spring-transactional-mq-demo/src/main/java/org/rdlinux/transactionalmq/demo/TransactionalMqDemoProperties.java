package org.rdlinux.transactionalmq.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事务消息 demo 配置。
 */
@ConfigurationProperties(prefix = "demo.transactional-mq")
public class TransactionalMqDemoProperties {

    /**
     * 是否在应用启动后写入并派发一条测试消息。
     */
    private boolean runOnStartup;
    /**
     * 测试消息目标。RabbitMQ 下可填写队列名，或使用 exchange:routingKey 格式。
     */
    private String destination = "spring.transactional.mq.demo";
    /**
     * 是否自动声明默认测试队列。
     */
    private boolean autoDeclareQueue = true;
    /**
     * 默认测试队列名。
     */
    private String queueName = "spring.transactional.mq.demo";
    /**
     * 是否启用 demo 消费者。
     */
    private boolean enableDemoConsumer = true;
    /**
     * demo 消费者最小并发。
     */
    private int consumerMinConcurrency = 1;
    /**
     * demo 消费者最大并发。
     */
    private int consumerMaxConcurrency = 2;

    /**
     * 是否在应用启动后写入并派发一条测试消息。
     *
     * @return 是否执行启动测试
     */
    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    /**
     * 设置是否在应用启动后写入并派发一条测试消息。
     *
     * @param runOnStartup 是否执行启动测试
     */
    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    /**
     * 获取测试消息目标。
     *
     * @return 测试消息目标
     */
    public String getDestination() {
        return destination;
    }

    /**
     * 设置测试消息目标。
     *
     * @param destination 测试消息目标
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * 是否自动声明默认测试队列。
     *
     * @return 是否自动声明默认测试队列
     */
    public boolean isAutoDeclareQueue() {
        return autoDeclareQueue;
    }

    /**
     * 设置是否自动声明默认测试队列。
     *
     * @param autoDeclareQueue 是否自动声明默认测试队列
     */
    public void setAutoDeclareQueue(boolean autoDeclareQueue) {
        this.autoDeclareQueue = autoDeclareQueue;
    }

    /**
     * 获取默认测试队列名。
     *
     * @return 默认测试队列名
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * 设置默认测试队列名。
     *
     * @param queueName 默认测试队列名
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * 是否启用 demo 消费者。
     *
     * @return 是否启用 demo 消费者
     */
    public boolean isEnableDemoConsumer() {
        return enableDemoConsumer;
    }

    /**
     * 设置是否启用 demo 消费者。
     *
     * @param enableDemoConsumer 是否启用 demo 消费者
     */
    public void setEnableDemoConsumer(boolean enableDemoConsumer) {
        this.enableDemoConsumer = enableDemoConsumer;
    }

    /**
     * 获取 demo 消费者最小并发。
     *
     * @return demo 消费者最小并发
     */
    public int getConsumerMinConcurrency() {
        return consumerMinConcurrency;
    }

    /**
     * 设置 demo 消费者最小并发。
     *
     * @param consumerMinConcurrency demo 消费者最小并发
     */
    public void setConsumerMinConcurrency(int consumerMinConcurrency) {
        this.consumerMinConcurrency = consumerMinConcurrency;
    }

    /**
     * 获取 demo 消费者最大并发。
     *
     * @return demo 消费者最大并发
     */
    public int getConsumerMaxConcurrency() {
        return consumerMaxConcurrency;
    }

    /**
     * 设置 demo 消费者最大并发。
     *
     * @param consumerMaxConcurrency demo 消费者最大并发
     */
    public void setConsumerMaxConcurrency(int consumerMaxConcurrency) {
        this.consumerMaxConcurrency = consumerMaxConcurrency;
    }
}
