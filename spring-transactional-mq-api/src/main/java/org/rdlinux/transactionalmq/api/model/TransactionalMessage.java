package org.rdlinux.transactionalmq.api.model;

import java.util.HashMap;
import java.util.Map;

import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MqType;

/**
 * 事务消息载体。
 *
 * @param <T> 消息负载类型
 */
public class TransactionalMessage<T> extends BaseEntity<TransactionalMessage<T>> {

    /**
     * 消息键。
     */
    private String messageKey;
    /**
     * 生产者编码。
     */
    private String producerCode;
    /**
     * MQ 类型。
     */
    private MqType mqType;
    /**
     * 目标信息。
     */
    private String destination;
    /**
     * 路由信息。
     */
    private String route;
    /**
     * 分片键。
     */
    private String shardingKey;
    /**
     * 消息负载。
     */
    private T payload;
    /**
     * 扩展消息头。
     */
    private Map<String, String> headers = new HashMap<String, String>();
    /**
     * 业务键。
     */
    private String bizKey;

    /**
     * 获取消息键。
     *
     * @return 消息键
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * 设置消息键。
     *
     * @param messageKey 消息键
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setMessageKey(String messageKey) {
        this.messageKey = messageKey;
        return castSelf();
    }

    /**
     * 获取生产者编码。
     *
     * @return 生产者编码
     */
    public String getProducerCode() {
        return producerCode;
    }

    /**
     * 设置生产者编码。
     *
     * @param producerCode 生产者编码
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setProducerCode(String producerCode) {
        this.producerCode = producerCode;
        return castSelf();
    }

    /**
     * 获取 MQ 类型。
     *
     * @return MQ 类型
     */
    public MqType getMqType() {
        return mqType;
    }

    /**
     * 设置 MQ 类型。
     *
     * @param mqType MQ 类型
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setMqType(MqType mqType) {
        this.mqType = mqType;
        return castSelf();
    }

    /**
     * 获取目标信息。
     *
     * @return 目标信息
     */
    public String getDestination() {
        return destination;
    }

    /**
     * 设置目标信息。
     *
     * @param destination 目标信息
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setDestination(String destination) {
        this.destination = destination;
        return castSelf();
    }

    /**
     * 获取路由信息。
     *
     * @return 路由信息
     */
    public String getRoute() {
        return route;
    }

    /**
     * 设置路由信息。
     *
     * @param route 路由信息
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setRoute(String route) {
        this.route = route;
        return castSelf();
    }

    /**
     * 获取分片键。
     *
     * @return 分片键
     */
    public String getShardingKey() {
        return shardingKey;
    }

    /**
     * 设置分片键。
     *
     * @param shardingKey 分片键
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setShardingKey(String shardingKey) {
        this.shardingKey = shardingKey;
        return castSelf();
    }

    /**
     * 获取消息负载。
     *
     * @return 消息负载
     */
    public T getPayload() {
        return payload;
    }

    /**
     * 设置消息负载。
     *
     * @param payload 消息负载
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setPayload(T payload) {
        this.payload = payload;
        return castSelf();
    }

    /**
     * 获取扩展消息头。
     *
     * @return 消息头
     */
    public Map<String, String> getHeaders() {
        return new HashMap<String, String>(headers);
    }

    /**
     * 设置扩展消息头。
     *
     * @param headers 消息头
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setHeaders(Map<String, String> headers) {
        if (headers == null) {
            this.headers = new HashMap<String, String>();
        } else {
            this.headers = new HashMap<String, String>(headers);
        }
        return castSelf();
    }

    /**
     * 获取业务键。
     *
     * @return 业务键
     */
    public String getBizKey() {
        return bizKey;
    }

    /**
     * 设置业务键。
     *
     * @param bizKey 业务键
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setBizKey(String bizKey) {
        this.bizKey = bizKey;
        return castSelf();
    }
}
