package org.rdlinux.transactionalmq.starter.config;

import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.springframework.beans.factory.InitializingBean;

/**
 * 事务消息启动校验器。
 */
public class TransactionalMqStartupValidator implements InitializingBean {

    private final MqProducerRouter mqProducerRouter;

    /**
     * 构造启动校验器。
     *
     * @param mqProducerRouter MQ 生产者路由器
     */
    public TransactionalMqStartupValidator(MqProducerRouter mqProducerRouter) {
        this.mqProducerRouter = mqProducerRouter;
    }

    @Override
    public void afterPropertiesSet() {
        if (!this.mqProducerRouter.hasAdapters()) {
            throw new IllegalStateException("No MqProducerAdapter found. Please introduce at least one MQ implementation module.");
        }
    }
}
