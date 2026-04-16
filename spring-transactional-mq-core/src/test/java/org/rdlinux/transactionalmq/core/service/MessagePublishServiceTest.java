package org.rdlinux.transactionalmq.core.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.model.SendResult;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;

/**
 * 消息发布服务测试。
 */
public class MessagePublishServiceTest {

    @Test
    public void publishShouldSaveRecordAndReturnAcceptedResult() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        MessagePayloadSerializer serializer = new MessagePayloadSerializer() {
            @Override
            public String serialize(Object payload) {
                return "serialized-" + payload;
            }

            @Override
            public <T> T deserialize(String payloadText, Type targetType) {
                return null;
            }
        };
        MessagePublishService service = new MessagePublishService(repository, serializer);

        TransactionalMessage<String> message = new TransactionalMessage<String>()
            .setId("msg-1")
            .setMessageKey("message-key-1")
            .setProducerCode("producer-1")
            .setMqType(MqType.RABBITMQ)
            .setDestination("demo.exchange")
            .setRoute("demo.route")
            .setShardingKey("order-1")
            .setPayload("payload-value")
            .setBizKey("biz-1");

        SendResult result = service.publish(message);

        Assert.assertTrue(result.isAccepted());
        Assert.assertEquals(SendResult.RESULT_CODE_ACCEPTED, result.getResultCode());
        Assert.assertEquals("507f1f77bcf86cd799439011", result.getId());
        Assert.assertEquals("message-key-1", result.getMessageKey());
        Assert.assertNotNull(repository.savedRecord);
        Assert.assertEquals("507f1f77bcf86cd799439011", repository.savedRecord.getId());
        Assert.assertEquals("serialized-payload-value", repository.savedRecord.getPayloadText());
        Assert.assertEquals("producer-1", repository.savedRecord.getProducerCode());
        Assert.assertEquals("demo.exchange", repository.savedRecord.getDestination());
        Assert.assertEquals("demo.route", repository.savedRecord.getRoute());
        Assert.assertEquals("order-1", repository.savedRecord.getShardingKey());
    }

    @Test
    public void transactionalMessageRecordFromShouldNotGenerateSeparateMessageIdentifier() {
        TransactionalMessage<String> message = new TransactionalMessage<String>()
            .setId("api-id")
            .setMessageKey("message-key-2")
            .setProducerCode("producer-2")
            .setDestination("topic-demo")
            .setRoute("tag-a")
            .setShardingKey("order-2")
            .setPayload("payload-value");

        TransactionalMessageRecord record = TransactionalMessageRecord.from(message, "serialized");

        Assert.assertNull(record.getId());
        Assert.assertEquals("message-key-2", record.getMessageKey());
        Assert.assertEquals("topic-demo", record.getDestination());
        Assert.assertEquals("tag-a", record.getRoute());
        Assert.assertEquals("order-2", record.getShardingKey());
        Assert.assertEquals("serialized", record.getPayloadText());
    }

    private static class CapturingTransactionalMessageRepository implements TransactionalMessageRepository {

        private TransactionalMessageRecord savedRecord;

        @Override
        public TransactionalMessageRecord save(TransactionalMessageRecord record) {
            record.setId("507f1f77bcf86cd799439011");
            this.savedRecord = record;
            return record;
        }

        @Override
        public List<TransactionalMessageRecord> findDispatchCandidates(int limit) {
            return Collections.emptyList();
        }

        @Override
        public TransactionalMessageRecord claimDispatchMessage(TransactionalMessageRecord record) {
            return null;
        }

        @Override
        public void markDispatchSuccess(List<TransactionalMessageRecord> records) {
        }

        @Override
        public void markDispatchFailed(List<TransactionalMessageRecord> records) {
        }

        @Override
        public int deleteSuccessMessages(Date cleanupBefore, int limit) {
            return 0;
        }
    }
}
