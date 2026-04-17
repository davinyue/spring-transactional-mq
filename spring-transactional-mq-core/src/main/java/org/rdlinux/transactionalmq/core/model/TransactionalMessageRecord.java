package org.rdlinux.transactionalmq.core.model;

import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * core 层事务消息持久化记录
 */
public class TransactionalMessageRecord extends BaseEntity<TransactionalMessageRecord> {

    /**
     * 消息键
     */
    private String messageKey;
    /**
     * 生产者编码
     */
    private String producerCode;
    /**
     * MQ 类型
     */
    private MqType mqType;
    /**
     * 目标信息
     */
    private String destination;
    /**
     * 路由信息
     */
    private String route;
    /**
     * 分片键
     */
    private String shardingKey;
    /**
     * 负载文本
     */
    private String payloadText;
    /**
     * 扩展消息头
     */
    private Map<String, String> headers = new HashMap<String, String>();
    /**
     * 业务键
     */
    private String bizKey;
    /**
     * 消息状态
     */
    private MessageStatus messageStatus;
    /**
     * 下次派发时间
     */
    private Date nextDispatchTime;
    /**
     * 父消息 id
     */
    private String parentId;
    /**
     * 根消息 id
     */
    private String rootId;
    /**
     * 派发实例标识
     */
    private String dispatchOwner;
    /**
     * 派发令牌
     */
    private String dispatchToken;
    /**
     * 派发租约过期时间
     */
    private Date dispatchExpireTime;

    /**
     * 从 API 消息创建记录对象
     *
     * <p>消息唯一标识统一使用 {@code id}，由存储层在入库时生成</p>
     *
     * @param message     API 消息
     * @param payloadText 负载文本
     * @param <T>         负载类型
     * @return 记录对象
     */
    public static <T> TransactionalMessageRecord from(MqType mqType, TransactionalMessage<T> message,
                                                      String payloadText) {
        TransactionalMessageRecord record = new TransactionalMessageRecord();
        if (message != null) {
            record.setMessageKey(message.getMessageKey());
            record.setProducerCode(message.getProducerCode());
            record.setMqType(mqType);
            record.setDestination(message.getDestination());
            record.setRoute(message.getRoute());
            record.setShardingKey(message.getShardingKey());
            record.setHeaders(message.getHeaders());
            record.setBizKey(message.getBizKey());
        }
        record.setPayloadText(payloadText);
        record.setMessageStatus(MessageStatus.INIT);
        record.setNextDispatchTime(new Date());
        return record;
    }

    /**
     * 从 API 消息和父消息上下文创建记录对象
     *
     * <p>该方法只继承消息链路信息，不会复制父消息的业务内容</p>
     *
     * @param message       API 消息
     * @param payloadText   负载文本
     * @param parentContext 父消息上下文
     * @param <T>           负载类型
     * @return 记录对象
     */
    public static <T> TransactionalMessageRecord from(MqType mqType, TransactionalMessage<T> message,
                                                      String payloadText, ConsumeContext parentContext) {
        TransactionalMessageRecord record = from(mqType, message, payloadText);
        if (parentContext != null) {
            record.setParentId(parentContext.getId());
            if (parentContext.getRootId() == null || parentContext.getRootId().trim().isEmpty()) {
                record.setRootId(parentContext.getId());
            } else {
                record.setRootId(parentContext.getRootId());
            }
        }
        return record;
    }

    /**
     * 获取消息键
     *
     * @return 消息键
     */
    public String getMessageKey() {
        return this.messageKey;
    }

    /**
     * 设置消息键
     *
     * @param messageKey 消息键
     */
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * 获取生产者编码
     *
     * @return 生产者编码
     */
    public String getProducerCode() {
        return this.producerCode;
    }

    /**
     * 设置生产者编码
     *
     * @param producerCode 生产者编码
     */
    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    /**
     * 获取 MQ 类型
     *
     * @return MQ 类型
     */
    public MqType getMqType() {
        return this.mqType;
    }

    /**
     * 设置 MQ 类型
     *
     * @param mqType MQ 类型
     */
    public void setMqType(MqType mqType) {
        this.mqType = mqType;
    }

    /**
     * 获取目标信息
     *
     * @return 目标信息
     */
    public String getDestination() {
        return this.destination;
    }

    /**
     * 设置目标信息
     *
     * @param destination 目标信息
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    public String getRoute() {
        return this.route;
    }

    /**
     * 设置路由信息
     *
     * @param route 路由信息
     */
    public void setRoute(String route) {
        this.route = route;
    }

    /**
     * 获取分片键
     *
     * @return 分片键
     */
    public String getShardingKey() {
        return this.shardingKey;
    }

    /**
     * 设置分片键
     *
     * @param shardingKey 分片键
     */
    public void setShardingKey(String shardingKey) {
        this.shardingKey = shardingKey;
    }

    /**
     * 获取负载文本
     *
     * @return 负载文本
     */
    public String getPayloadText() {
        return this.payloadText;
    }

    /**
     * 设置负载文本
     *
     * @param payloadText 负载文本
     */
    public void setPayloadText(String payloadText) {
        this.payloadText = payloadText;
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
     */
    public void setHeaders(Map<String, String> headers) {
        if (headers == null) {
            this.headers = new HashMap<String, String>();
            return;
        }
        this.headers = new HashMap<String, String>(headers);
    }

    /**
     * 获取业务键
     *
     * @return 业务键
     */
    public String getBizKey() {
        return this.bizKey;
    }

    /**
     * 设置业务键
     *
     * @param bizKey 业务键
     */
    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    /**
     * 获取消息状态
     *
     * @return 消息状态
     */
    public MessageStatus getMessageStatus() {
        return this.messageStatus;
    }

    /**
     * 设置消息状态
     *
     * @param messageStatus 消息状态
     */
    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    /**
     * 获取下次派发时间
     *
     * @return 下次派发时间
     */
    public Date getNextDispatchTime() {
        return this.nextDispatchTime;
    }

    /**
     * 设置下次派发时间
     *
     * @param nextDispatchTime 下次派发时间
     */
    public void setNextDispatchTime(Date nextDispatchTime) {
        this.nextDispatchTime = nextDispatchTime;
    }

    /**
     * 获取父消息 id
     *
     * @return 父消息 id
     */
    public String getParentId() {
        return this.parentId;
    }

    /**
     * 设置父消息 id
     *
     * @param parentId 父消息 id
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * 获取根消息 id
     *
     * @return 根消息 id
     */
    public String getRootId() {
        return this.rootId;
    }

    /**
     * 设置根消息 id
     *
     * @param rootId 根消息 id
     */
    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    /**
     * 获取派发实例标识
     *
     * @return 派发实例标识
     */
    public String getDispatchOwner() {
        return this.dispatchOwner;
    }

    /**
     * 设置派发实例标识
     *
     * @param dispatchOwner 派发实例标识
     */
    public void setDispatchOwner(String dispatchOwner) {
        this.dispatchOwner = dispatchOwner;
    }

    /**
     * 获取派发令牌
     *
     * @return 派发令牌
     */
    public String getDispatchToken() {
        return this.dispatchToken;
    }

    /**
     * 设置派发令牌
     *
     * @param dispatchToken 派发令牌
     */
    public void setDispatchToken(String dispatchToken) {
        this.dispatchToken = dispatchToken;
    }

    /**
     * 获取派发租约过期时间
     *
     * @return 派发租约过期时间
     */
    public Date getDispatchExpireTime() {
        return this.dispatchExpireTime;
    }

    /**
     * 设置派发租约过期时间
     *
     * @param dispatchExpireTime 派发租约过期时间
     */
    public void setDispatchExpireTime(Date dispatchExpireTime) {
        this.dispatchExpireTime = dispatchExpireTime;
    }

}
