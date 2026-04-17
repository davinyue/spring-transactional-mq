package org.rdlinux.transactionalmq.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

/**
 * RabbitMQ 生产者适配器
 *
 * <p>新 API 使用 {@code destination} 表达 exchange/queue，使用 {@code route} 表达 routingKey
 * 为兼容早期写法，{@code destination=exchange:routingKey} 仍会被解析为 exchange 和 routingKey</p>
 */
@Slf4j
public class RabbitMqProducerAdapter implements MqProducerAdapter {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 构造 RabbitMQ 生产者适配器
     *
     * @param rabbitTemplate RabbitMQ 模板
     */
    public RabbitMqProducerAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public MqType supportMqType() {
        return MqType.RABBITMQ;
    }

    @Override
    public void send(DispatchMessage message) {
        String destination = message.getDestination();
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("destination must not be blank");
        }
        DestinationTarget target = this.resolveDestination(destination.trim(), message.getRoute());
        byte[] payloadBytes = RabbitMqPayloadCodec.gzip(message.getPayloadText());
        this.rabbitTemplate.convertAndSend(target.exchange, target.routingKey, payloadBytes,
                originalMessage -> {
                    MessageProperties messageProperties = originalMessage.getMessageProperties();
                    messageProperties.setContentEncoding("gzip");
                    messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
                        messageProperties.setHeader(entry.getKey(), entry.getValue());
                    }
                    messageProperties.setMessageId(message.getId());
                    messageProperties.setHeader("messageKey", message.getMessageKey());
                    messageProperties.setHeader("producerCode", message.getProducerCode());
                    messageProperties.setHeader("mqType", message.getMqType() == null ? null : message.getMqType().name());
                    messageProperties.setHeader("bizKey", message.getBizKey());
                    messageProperties.setHeader("route", message.getRoute());
                    messageProperties.setHeader("shardingKey", message.getShardingKey());
                    messageProperties.setHeader("parentId", message.getParentId());
                    messageProperties.setHeader("rootId", message.getRootId());
                    return originalMessage;
                });
        log.info("消息发送成功, 交换机{}, 路由键{}, 消息id{}", target.exchange, target.routingKey, message.getId());
    }

    private DestinationTarget resolveDestination(String destination, String route) {
        if (route != null && !route.trim().isEmpty()) {
            return new DestinationTarget(destination, route.trim());
        }
        int separatorIndex = destination.indexOf(':');
        if (separatorIndex < 0) {
            return new DestinationTarget("", destination);
        }
        String exchange = destination.substring(0, separatorIndex).trim();
        String routingKey = destination.substring(separatorIndex + 1).trim();
        if (exchange.isEmpty() || routingKey.isEmpty()) {
            throw new IllegalArgumentException("destination must be exchange:routingKey");
        }
        return new DestinationTarget(exchange, routingKey);
    }

    private static final class DestinationTarget {

        private final String exchange;
        private final String routingKey;

        private DestinationTarget(String exchange, String routingKey) {
            this.exchange = exchange;
            this.routingKey = routingKey;
        }
    }
}
