package org.rdlinux.transactionalmq.core.model;

import java.util.HashMap;
import java.util.Map;

import org.rdlinux.transactionalmq.common.enums.MqType;

/**
 * 待派发消息。
 */
public class DispatchMessage {

    /**
     * 消息主键。
     */
    private String id;
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
     * 负载文本。
     */
    private String payloadText;
    /**
     * 扩展消息头。
     */
    private Map<String, String> headers = new HashMap<String, String>();
    /**
     * 业务键。
     */
    private String bizKey;
    /**
     * 父消息 id。
     */
    private String parentId;
    /**
     * 根消息 id。
     */
    private String rootId;

    /**
     * 从消息记录创建派发对象。
     *
     * @param record 消息记录
     * @return 派发对象
     */
    public static DispatchMessage from(TransactionalMessageRecord record) {
        DispatchMessage message = new DispatchMessage();
        if (record != null) {
            message.setId(record.getId());
            message.setMessageKey(record.getMessageKey());
            message.setProducerCode(record.getProducerCode());
            message.setMqType(record.getMqType());
            message.setDestination(record.getDestination());
            message.setRoute(record.getRoute());
            message.setShardingKey(record.getShardingKey());
            message.setPayloadText(record.getPayloadText());
            message.setHeaders(record.getHeaders());
            message.setBizKey(record.getBizKey());
            message.setParentId(record.getParentId());
            message.setRootId(record.getRootId());
        }
        return message;
    }

    /**
     * 获取消息主键。
     *
     * @return 消息主键
     */
    public String getId() {
        return id;
    }

    /**
     * 设置消息主键。
     *
     * @param id 消息主键
     */
    public void setId(String id) {
        this.id = id;
    }

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
     */
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
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
     */
    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
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
     */
    public void setMqType(MqType mqType) {
        this.mqType = mqType;
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
     */
    public void setDestination(String destination) {
        this.destination = destination;
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
     */
    public void setRoute(String route) {
        this.route = route;
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
     */
    public void setShardingKey(String shardingKey) {
        this.shardingKey = shardingKey;
    }

    /**
     * 获取负载文本。
     *
     * @return 负载文本
     */
    public String getPayloadText() {
        return payloadText;
    }

    /**
     * 设置负载文本。
     *
     * @param payloadText 负载文本
     */
    public void setPayloadText(String payloadText) {
        this.payloadText = payloadText;
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
     */
    public void setHeaders(Map<String, String> headers) {
        if (headers == null) {
            this.headers = new HashMap<String, String>();
            return;
        }
        this.headers = new HashMap<String, String>(headers);
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
     */
    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    /**
     * 获取父消息 id。
     *
     * @return 父消息 id
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * 设置父消息 id。
     *
     * @param parentId 父消息 id
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * 获取根消息 id。
     *
     * @return 根消息 id
     */
    public String getRootId() {
        return rootId;
    }

    /**
     * 设置根消息 id。
     *
     * @param rootId 根消息 id
     */
    public void setRootId(String rootId) {
        this.rootId = rootId;
    }
}
