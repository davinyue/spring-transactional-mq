package org.rdlinux.transactionalmq.api;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.SendResult;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.producer.TransactionalMessageSender;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MqType;

/**
 * API 模型与接口测试。
 */
public class TransactionalMessageApiTest {

    /**
     * 验证事务消息支持链式设置。
     */
    @Test
    public void transactionalMessageShouldSupportChainedSetters() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("traceId", "trace-1");

        TransactionalMessage<String> message = new TransactionalMessage<String>()
            .setId("msg-1")
            .setMessageKey("message-key-1")
            .setProducerCode("producer-1")
            .setMqType(MqType.RABBITMQ)
            .setDestination("demo.exchange")
            .setRoute("demo.route")
            .setShardingKey("order-1")
            .setPayload("payload-value")
            .setHeaders(headers)
            .setBizKey("biz-1");

        Assert.assertEquals("msg-1", message.getId());
        Assert.assertEquals("message-key-1", message.getMessageKey());
        Assert.assertEquals("producer-1", message.getProducerCode());
        Assert.assertEquals(MqType.RABBITMQ, message.getMqType());
        Assert.assertEquals("demo.exchange", message.getDestination());
        Assert.assertEquals("demo.route", message.getRoute());
        Assert.assertEquals("order-1", message.getShardingKey());
        Assert.assertEquals("payload-value", message.getPayload());
        Assert.assertEquals("trace-1", message.getHeaders().get("traceId"));
        Assert.assertEquals("biz-1", message.getBizKey());

        headers.put("traceId", "trace-2");
        Assert.assertEquals("trace-1", message.getHeaders().get("traceId"));

        Map<String, String> exposedHeaders = message.getHeaders();
        exposedHeaders.put("newKey", "newValue");
        Assert.assertFalse(message.getHeaders().containsKey("newKey"));
    }

    /**
     * 验证发送结果工厂方法可用。
     */
    @Test
    public void sendResultFactoryMethodsShouldWork() {
        SendResult accepted = SendResult.accepted("msg-1", "message-key-1");
        SendResult rejected = SendResult.rejected("message-key-2", "no route");

        Assert.assertEquals(SendResult.RESULT_CODE_ACCEPTED, accepted.getResultCode());
        Assert.assertTrue(accepted.isAccepted());
        Assert.assertEquals("msg-1", accepted.getId());
        Assert.assertEquals("message-key-1", accepted.getMessageKey());
        Assert.assertEquals(SendResult.RESULT_CODE_REJECTED, rejected.getResultCode());
        Assert.assertFalse(rejected.isAccepted());
        Assert.assertEquals("message-key-2", rejected.getMessageKey());
        Assert.assertEquals("no route", rejected.getDescription());
    }

    /**
     * 验证接口签名能够正常编译和使用。
     */
    @Test
    public void interfacesShouldCompileAndWork() {
        final String[] observedConsumerCode = new String[1];
        final String[] observedPayload = new String[1];

        TransactionalMessageConsumer<String> consumer = new TransactionalMessageConsumer<String>() {
            @Override
            public String getQueueName() {
                return "queue.consumer.1";
            }

            @Override
            public String consumerCode() {
                return "consumer-1";
            }

            @Override
            public void consume(ConsumeContext context, String payload) {
                observedConsumerCode[0] = context.getConsumerCode();
                observedPayload[0] = payload;
            }
        };

        TransactionalMessageSender sender = new TransactionalMessageSender() {
            @Override
            public <T> SendResult send(TransactionalMessage<T> message) {
                return SendResult.accepted("msg-2", message.getMessageKey());
            }
        };

        MessagePayloadSerializer serializer = new MessagePayloadSerializer() {
            @Override
            public String serialize(Object payload) {
                return payload == null ? null : payload.toString();
            }

            @Override
            public <T> T deserialize(String payloadText, Type targetType) {
                return null;
            }
        };

        TransactionalMessage<String> message = new TransactionalMessage<String>()
            .setMessageKey("message-key-1")
            .setPayload("payload-value");
        ConsumeContext context = new ConsumeContext()
            .setId("msg-1")
            .setMessageKey("message-key-1")
            .setConsumerCode(consumer.consumerCode());

        consumer.consume(context, message.getPayload());

        SendResult sendResult = sender.send(message);

        Assert.assertEquals("consumer-1", observedConsumerCode[0]);
        Assert.assertEquals("payload-value", observedPayload[0]);
        Assert.assertEquals("queue.consumer.1", consumer.getQueueName());
        Assert.assertEquals(1, consumer.getMinConcurrency());
        Assert.assertEquals(1, consumer.getMaxConcurrency());
        Assert.assertTrue(sendResult.isAccepted());
        Assert.assertEquals(SendResult.RESULT_CODE_ACCEPTED, sendResult.getResultCode());
        Assert.assertEquals("message-key-1", sendResult.getMessageKey());
        Assert.assertEquals("payload-value", serializer.serialize(message.getPayload()));

        Type targetType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { String.class };
            }

            @Override
            public Type getRawType() {
                return java.util.List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        Assert.assertNull(serializer.deserialize("[]", targetType));
        Assert.assertNull(serializer.deserialize("[]", String.class));
    }
}
