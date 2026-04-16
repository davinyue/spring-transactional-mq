package org.rdlinux.transactionalmq.core.mq;

import org.rdlinux.transactionalmq.core.model.DispatchMessage;

/**
 * MQ 生产者适配器 SPI。
 */
public interface MqProducerAdapter {

    /**
     * 发送待派发消息到 MQ。
     *
     * @param message 待派发消息
     */
    void send(DispatchMessage message);
}
