package org.rdlinux.transactionalmq.api.model;

import lombok.Getter;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 事务消息载体
 *
 * @param <T> 消息负载类型
 */
public class TransactionalMessage<T> extends BaseEntity<TransactionalMessage<T>> {

    /**
     * 消息键
     */
    @Getter
    private String messageKey;
    /**
     * 生产者编码
     */
    @Getter
    private String producerCode;
    /**
     * 目标信息
     */
    @Getter
    private String destination;
    /**
     * 路由信息
     */
    @Getter
    private String route;
    /**
     * 分片键
     */
    @Getter
    private String shardingKey;
    /**
     * 消息负载
     */
    @Getter
    private T payload;
    /**
     * 扩展消息头
     */
    private Map<String, String> headers = new HashMap<String, String>();
    /**
     * 业务键
     */
    @Getter
    private String bizKey;

    /**
     * 设置消息键
     *
     * @param messageKey 消息键
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setMessageKey(String messageKey) {
        this.messageKey = messageKey;
        return this.castSelf();
    }

    /**
     * 设置生产者编码
     *
     * @param producerCode 生产者编码
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setProducerCode(String producerCode) {
        this.producerCode = producerCode;
        return this.castSelf();
    }

    /**
     * 设置目标信息
     *
     * @param destination 目标信息
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setDestination(String destination) {
        this.destination = destination;
        return this.castSelf();
    }

    /**
     * 设置路由信息
     *
     * @param route 路由信息
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setRoute(String route) {
        this.route = route;
        return this.castSelf();
    }

    /**
     * 设置分片键
     *
     * @param shardingKey 分片键
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setShardingKey(String shardingKey) {
        this.shardingKey = shardingKey;
        return this.castSelf();
    }

    /**
     * 设置消息负载
     *
     * @param payload 消息负载
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setPayload(T payload) {
        this.payload = payload;
        return this.castSelf();
    }

    /**
     * 获取扩展消息头
     *
     * @return 消息头
     */
    public Map<String, String> getHeaders() {
        return new HashMap<String, String>(this.headers);
    }

    /**
     * 设置扩展消息头
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
        return this.castSelf();
    }

    /**
     * 设置业务键
     *
     * @param bizKey 业务键
     * @return 当前消息对象
     */
    public TransactionalMessage<T> setBizKey(String bizKey) {
        this.bizKey = bizKey;
        return this.castSelf();
    }
}
