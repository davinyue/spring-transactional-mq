package org.rdlinux.transactionalmq.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka 生产者适配器。
 */
public class KafkaProducerAdapter implements MqProducerAdapter {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducerAdapter(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public MqType supportMqType() {
        return MqType.KAFKA;
    }

    @Override
    public void send(DispatchMessage message) {
        String topic = message.getDestination();
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("destination must not be blank");
        }
        byte[] payloadBytes = KafkaPayloadCodec.gzip(message.getPayloadText());
        ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(topic.trim(),
                message.getRoute(), payloadBytes);
        for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
            this.addHeader(record, entry.getKey(), entry.getValue());
        }
        this.addHeader(record, "contentEncoding", "gzip");
        this.addHeader(record, "messageId", message.getId());
        this.addHeader(record, "messageKey", message.getMessageKey());
        this.addHeader(record, "producerCode", message.getProducerCode());
        this.addHeader(record, "mqType", message.getMqType() == null ? null : message.getMqType().name());
        this.addHeader(record, "bizKey", message.getBizKey());
        this.addHeader(record, "route", message.getRoute());
        this.addHeader(record, "shardingKey", message.getShardingKey());
        this.addHeader(record, "parentId", message.getParentId());
        this.addHeader(record, "rootId", message.getRootId());
        this.kafkaTemplate.send(record);
    }

    private void addHeader(ProducerRecord<String, byte[]> record, String key, String value) {
        if (key == null || value == null) {
            return;
        }
        record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
    }
}
