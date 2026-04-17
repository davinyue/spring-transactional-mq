package org.rdlinux.transactionalmq.core.mq;

import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MQ 生产者路由器。
 */
public class MqProducerRouter {

    private final Map<MqType, MqProducerAdapter> adapters = new LinkedHashMap<MqType, MqProducerAdapter>();

    /**
     * 构造 MQ 生产者路由器。
     *
     * @param producerAdapters 生产者适配器集合
     */
    public MqProducerRouter(List<MqProducerAdapter> producerAdapters) {
        if (producerAdapters == null) {
            return;
        }
        for (MqProducerAdapter producerAdapter : producerAdapters) {
            if (producerAdapter == null) {
                continue;
            }
            MqType mqType = producerAdapter.supportMqType();
            if (mqType == null) {
                throw new IllegalStateException("MqProducerAdapter supportMqType must not be null");
            }
            if (this.adapters.containsKey(mqType)) {
                throw new IllegalStateException("duplicate MqProducerAdapter for mqType: " + mqType);
            }
            this.adapters.put(mqType, producerAdapter);
        }
    }

    /**
     * 按 mqType 路由发送消息。
     *
     * @param message 待发送消息
     */
    public void send(DispatchMessage message) {
        if (message == null || message.getMqType() == null) {
            throw new IllegalArgumentException("message mqType must not be null");
        }
        MqProducerAdapter producerAdapter = this.adapters.get(message.getMqType());
        if (producerAdapter == null) {
            throw new IllegalArgumentException("unsupported mqType: " + message.getMqType());
        }
        producerAdapter.send(message);
    }

    /**
     * 是否存在至少一个适配器。
     *
     * @return 是否存在适配器
     */
    public boolean hasAdapters() {
        return !this.adapters.isEmpty();
    }

    /**
     * 是否支持指定 mqType。
     *
     * @param mqType MQ 类型
     * @return 是否支持
     */
    public boolean supports(MqType mqType) {
        return mqType != null && this.adapters.containsKey(mqType);
    }
}
