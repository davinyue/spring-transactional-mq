package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 消息发布服务测试
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
        MessagePublishService service = new MessagePublishService(repository, serializer, null);

        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setId("msg-1")
                .setMessageKey("message-key-1")
                .setProducerCode("producer-1")
                .setDestination("demo.exchange")
                .setRoute("demo.route")
                .setShardingKey("order-1")
                .setPayload("payload-value")
                .setBizKey("biz-1");

        String messageId = service.send(MqType.RABBITMQ, message);

        Assert.assertNotNull(messageId);
        Assert.assertNotNull(repository.savedRecord);
        Assert.assertEquals(messageId, repository.savedRecord.getId());
        Assert.assertEquals(messageId, repository.savedRecord.getRootId());
        Assert.assertNull(repository.savedRecord.getParentId());
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

        TransactionalMessageRecord record = TransactionalMessageRecord.from(MqType.RABBITMQ, message, "serialized");

        Assert.assertNull(record.getId());
        Assert.assertEquals("message-key-2", record.getMessageKey());
        Assert.assertEquals("topic-demo", record.getDestination());
        Assert.assertEquals("tag-a", record.getRoute());
        Assert.assertEquals("order-2", record.getShardingKey());
        Assert.assertEquals("serialized", record.getPayloadText());
        Assert.assertNull(record.getParentId());
        Assert.assertNull(record.getRootId());
    }

    @Test
    public void sendWithParentShouldPropagateParentAndRootIdentifiers() {
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
        MessagePublishService service = new MessagePublishService(repository, serializer, null);
        TransactionalMessage<String> message = new TransactionalMessage<String>()
                .setMessageKey("message-key-3")
                .setPayload("payload-value-3");
        ConsumeContext parentContext = new ConsumeContext()
                .setId("parent-1")
                .setRootId("root-1")
                .setMessageKey("message-key-parent")
                .setConsumerCode("consumer-1");

        String childId = service.sendWithParent(MqType.RABBITMQ, message, parentContext);

        Assert.assertNotNull(childId);
        Assert.assertEquals(childId, repository.savedRecord.getId());
        Assert.assertEquals("parent-1", repository.savedRecord.getParentId());
        Assert.assertEquals("root-1", repository.savedRecord.getRootId());
    }

    @Test
    public void sendShouldBeTransactional() throws Exception {
        Method method = MessagePublishService.class.getMethod("send", MqType.class, TransactionalMessage.class);

        Assert.assertNotNull(method.getAnnotation(Transactional.class));
    }

    private static class CapturingTransactionalMessageRepository implements TransactionalMessageRepository {

        private TransactionalMessageRecord savedRecord;

        @Override
        public TransactionalMessageRecord save(TransactionalMessageRecord record) {
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
        public List<TransactionalMessageRecord> findSuccessCleanupCandidates(Date cleanupBefore, int limit) {
            return Collections.emptyList();
        }

        @Override
        public int archiveSuccessMessage(TransactionalMessageRecord record, Date cleanupBefore) {
            return 0;
        }
    }
}
