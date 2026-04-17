package org.rdlinux.transactionalmq.rabbitmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitMqProducerAdapterTest {

    @Test
    public void supportMqTypeShouldReturnRabbitMq() {
        RabbitMqProducerAdapter adapter = new RabbitMqProducerAdapter(mock(RabbitTemplate.class));

        assertEquals(MqType.RABBITMQ, adapter.supportMqType());
    }

    @Test
    public void sendShouldUseDefaultExchangeWhenDestinationHasNoColon() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMqProducerAdapter adapter = new RabbitMqProducerAdapter(rabbitTemplate);

        DispatchMessage message = new DispatchMessage();
        message.setDestination("queue.demo");
        message.setPayloadText("hello");

        adapter.send(message);

        verify(rabbitTemplate).convertAndSend(eq(""), eq("queue.demo"),
                org.mockito.ArgumentMatchers.<Object>argThat(payload -> {
                    assertArrayEquals(RabbitMqPayloadCodec.gzip("hello"), (byte[]) payload);
                    return true;
                }),
                org.mockito.ArgumentMatchers.<MessagePostProcessor>any());
    }

    @Test
    public void sendShouldUseExplicitExchangeAndRoutingKeyWhenDestinationHasColon() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMqProducerAdapter adapter = new RabbitMqProducerAdapter(rabbitTemplate);

        DispatchMessage message = new DispatchMessage();
        message.setMqType(MqType.RABBITMQ);
        message.setDestination("exchange.demo:queue.demo");
        message.setPayloadText("hello");

        adapter.send(message);

        verify(rabbitTemplate).convertAndSend(eq("exchange.demo"), eq("queue.demo"),
                org.mockito.ArgumentMatchers.<Object>argThat(payload -> {
                    assertArrayEquals(RabbitMqPayloadCodec.gzip("hello"), (byte[]) payload);
                    return true;
                }),
                org.mockito.ArgumentMatchers.<MessagePostProcessor>any());
    }

    @Test
    public void sendShouldUseRouteAsRoutingKeyWhenRouteIsConfigured() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMqProducerAdapter adapter = new RabbitMqProducerAdapter(rabbitTemplate);

        DispatchMessage message = new DispatchMessage();
        message.setDestination("exchange.demo");
        message.setRoute("route.demo");
        message.setPayloadText("hello");

        adapter.send(message);

        verify(rabbitTemplate).convertAndSend(eq("exchange.demo"), eq("route.demo"),
                org.mockito.ArgumentMatchers.<Object>argThat(payload -> {
                    assertArrayEquals(RabbitMqPayloadCodec.gzip("hello"), (byte[]) payload);
                    return true;
                }),
                org.mockito.ArgumentMatchers.<MessagePostProcessor>any());
    }

    @Test
    public void sendShouldRejectBlankDestination() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMqProducerAdapter adapter = new RabbitMqProducerAdapter(rabbitTemplate);

        DispatchMessage message = new DispatchMessage();
        message.setDestination("   ");
        message.setPayloadText("hello");

        try {
            adapter.send(message);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("destination must not be blank", ex.getMessage());
        }
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    public void sendShouldKeepReservedHeadersWhenBusinessHeadersContainSameKeys() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMqProducerAdapter adapter = new RabbitMqProducerAdapter(rabbitTemplate);

        DispatchMessage message = new DispatchMessage();
        message.setId("msg-2");
        message.setMessageKey("key-2");
        message.setProducerCode("producer-2");
        message.setMqType(MqType.RABBITMQ);
        message.setDestination("queue.demo");
        message.setRoute("route.demo");
        message.setShardingKey("order-2");
        message.setPayloadText("hello");
        message.setBizKey("biz-2");
        message.setParentId("parent-2");
        message.setRootId("root-2");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("messageKey", "shadow-key");
        headers.put("producerCode", "shadow-producer");
        headers.put("traceId", "trace-2");
        message.setHeaders(headers);

        final MessagePostProcessor[] holder = new MessagePostProcessor[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            holder[0] = invocation.getArgument(3);
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("queue.demo"), eq("route.demo"),
                org.mockito.ArgumentMatchers.<Object>argThat(payload -> {
                    assertArrayEquals(RabbitMqPayloadCodec.gzip("hello"), (byte[]) payload);
                    return true;
                }),
                org.mockito.ArgumentMatchers.<MessagePostProcessor>any());

        adapter.send(message);

        MessageProperties properties = new MessageProperties();
        Message original = new Message("body".getBytes(StandardCharsets.UTF_8), properties);
        Message processed = holder[0].postProcessMessage(original);

        assertEquals("gzip", processed.getMessageProperties().getContentEncoding());
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, processed.getMessageProperties().getContentType());
        assertEquals("msg-2", processed.getMessageProperties().getMessageId());
        assertEquals("key-2", processed.getMessageProperties().getHeaders().get("messageKey"));
        assertEquals("producer-2", processed.getMessageProperties().getHeaders().get("producerCode"));
        assertEquals("RABBITMQ", processed.getMessageProperties().getHeaders().get("mqType"));
        assertEquals("biz-2", processed.getMessageProperties().getHeaders().get("bizKey"));
        assertEquals("route.demo", processed.getMessageProperties().getHeaders().get("route"));
        assertEquals("order-2", processed.getMessageProperties().getHeaders().get("shardingKey"));
        assertEquals("parent-2", processed.getMessageProperties().getHeaders().get("parentId"));
        assertEquals("root-2", processed.getMessageProperties().getHeaders().get("rootId"));
        assertEquals("trace-2", processed.getMessageProperties().getHeaders().get("traceId"));
    }
}
