package org.rdlinux.transactionalmq.api.serialize;

import java.lang.reflect.Type;

/**
 * 消息负载序列化器
 */
public interface MessagePayloadSerializer {

    /**
     * 将对象序列化为文本
     *
     * @param payload 待序列化对象
     * @return 文本内容
     */
    String serialize(Object payload);

    /**
     * 将文本反序列化为对象
     *
     * @param payloadText 文本内容
     * @param targetType  目标类型
     * @param <T>         目标对象类型
     * @return 反序列化对象
     */
    <T> T deserialize(String payloadText, Type targetType);

    /**
     * 将文本反序列化为对象
     *
     * @param payloadText 文本内容
     * @param targetType  目标类型
     * @param <T>         目标对象类型
     * @return 反序列化对象
     */
    default <T> T deserialize(String payloadText, Class<T> targetType) {
        return this.deserialize(payloadText, (Type) targetType);
    }
}
