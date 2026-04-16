package org.rdlinux.transactionalmq.starter.config;

import java.lang.reflect.Type;

import org.rdlinux.luava.json.JacksonUtils;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;

/**
 * 基于 luava-json 的默认消息负载序列化器。
 */
public class LuavaJsonMessagePayloadSerializer implements MessagePayloadSerializer {

    @Override
    public String serialize(Object payload) {
        return JacksonUtils.toJsonString(payload);
    }

    @Override
    public <T> T deserialize(String payloadText, Type targetType) {
        return JacksonUtils.conversion(payloadText, targetType);
    }

    @Override
    public <T> T deserialize(String payloadText, Class<T> targetType) {
        return JacksonUtils.conversion(payloadText, targetType);
    }
}
