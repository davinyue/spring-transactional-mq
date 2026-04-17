package org.rdlinux.transactionalmq.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class KafkaProducerAdapterTest {

    @Test
    public void supportMqTypeShouldReturnKafka() {
        KafkaProducerAdapter adapter = new KafkaProducerAdapter(mock(KafkaTemplate.class));

        assertEquals(MqType.KAFKA, adapter.supportMqType());
    }

    @Test
    public void sendShouldUseDestinationAsTopicAndRouteAsKey() {
        KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaProducerAdapter adapter = new KafkaProducerAdapter(kafkaTemplate);
        DispatchMessage message = new DispatchMessage();
        message.setDestination("topic.demo");
        message.setRoute("route.demo");
        message.setPayloadText("hello");

        adapter.send(message);

        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.argThat((ProducerRecord<String, byte[]> record) -> {
            assertEquals("topic.demo", record.topic());
            assertEquals("route.demo", record.key());
            assertArrayEquals(KafkaPayloadCodec.gzip("hello"), record.value());
            return true;
        }));
    }

    @Test
    public void sendShouldRejectBlankDestination() {
        KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaProducerAdapter adapter = new KafkaProducerAdapter(kafkaTemplate);
        DispatchMessage message = new DispatchMessage();
        message.setDestination("   ");
        message.setPayloadText("hello");

        try {
            adapter.send(message);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("destination must not be blank", ex.getMessage());
        }
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    public void sendShouldKeepReservedHeadersWhenBusinessHeadersContainSameKeys() {
        KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaProducerAdapter adapter = new KafkaProducerAdapter(kafkaTemplate);
        DispatchMessage message = new DispatchMessage();
        message.setId("msg-2");
        message.setMessageKey("key-2");
        message.setProducerCode("producer-2");
        message.setMqType(MqType.KAFKA);
        message.setDestination("topic.demo");
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

        adapter.send(message);

        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.argThat((ProducerRecord<String, byte[]> record) -> {
            assertEquals("msg-2", header(record, "messageId"));
            assertEquals("key-2", header(record, "messageKey"));
            assertEquals("producer-2", header(record, "producerCode"));
            assertEquals("KAFKA", header(record, "mqType"));
            assertEquals("biz-2", header(record, "bizKey"));
            assertEquals("route.demo", header(record, "route"));
            assertEquals("order-2", header(record, "shardingKey"));
            assertEquals("parent-2", header(record, "parentId"));
            assertEquals("root-2", header(record, "rootId"));
            assertEquals("trace-2", header(record, "traceId"));
            return true;
        }));
    }

    private String header(ProducerRecord<String, byte[]> record, String key) {
        org.apache.kafka.common.header.Header header = record.headers().lastHeader(key);
        return header == null || header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
