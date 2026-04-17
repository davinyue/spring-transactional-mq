package org.rdlinux.transactionalmq.core.mq;

import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;

/**
 * MQ 生产者适配器 SPI
 */
public interface MqProducerAdapter {

    /**
     * 当前适配器支持的 MQ 类型。
     *
     * @return MQ 类型
     */
    MqType supportMqType();

    /**
     * 发送待派发消息到 MQ
     *
     * @param message 待派发消息
     */
    void send(DispatchMessage message);
}
