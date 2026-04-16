package org.rdlinux.transactionalmq.demo;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.rdlinux.ezmybatis.core.EzDelete;
import org.rdlinux.ezmybatis.core.EzQuery;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.sqlstruct.Select;
import org.rdlinux.ezmybatis.core.sqlstruct.table.EntityTable;
import org.rdlinux.transactionalmq.api.consumer.TransactionalMessageConsumer;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.api.model.SendResult;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.core.service.MessageDispatchService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.core.service.TransactionalMessageCleanupService;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqConsumerInvoker;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqPayloadCodec;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageHistoryEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.MessageSendLogEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageHistoryEntity;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

/**
 * 真实 Oracle 与 RabbitMQ 发送消费集成测试。
 *
 * <p>该测试会连接真实外部环境并写入测试数据，默认不执行。需要运行时显式增加：
 * {@code -DrealMqTest=true}。</p>
 */
@SpringBootTest(classes = TransactionalMqDemoApplication.class, properties = {
        "demo.transactional-mq.run-on-startup=false",
        "demo.transactional-mq.enable-demo-consumer=false"
})
@ActiveProfiles("real")
@EnabledIfSystemProperty(named = "realMqTest", matches = "true")
public class TransactionalMqRealSendConsumeTest {

    private static final String TEST_MESSAGE_KEY_PREFIX = "junit-%";
    private static final String TEST_CONSUMER_CODE = "spring-transactional-mq-demo-test-consumer";
    private static final EntityTable CONSUMED_MESSAGE_TABLE = EntityTable.of(ConsumedMessageEntity.class);
    private static final EntityTable CONSUMED_MESSAGE_HISTORY_TABLE =
            EntityTable.of(ConsumedMessageHistoryEntity.class);
    private static final EntityTable MESSAGE_SEND_LOG_TABLE = EntityTable.of(MessageSendLogEntity.class);
    private static final EntityTable TRANSACTIONAL_MESSAGE_TABLE = EntityTable.of(TransactionalMessageEntity.class);
    private static final EntityTable TRANSACTIONAL_MESSAGE_HISTORY_TABLE =
            EntityTable.of(TransactionalMessageHistoryEntity.class);

    @Autowired
    private MessagePublishService messagePublishService;
    @Autowired
    private MessageDispatchService messageDispatchService;
    @Autowired
    private TransactionalMessageCleanupService transactionalMessageCleanupService;
    @Autowired
    private ConsumeIdempotentService consumeIdempotentService;
    @Autowired
    private RabbitMqConsumerInvoker rabbitMqConsumerInvoker;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MessagePayloadSerializer messagePayloadSerializer;
    @Autowired
    private TransactionalMqDemoProperties properties;
    @Autowired
    private EzDao ezDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 清理真实测试环境中的历史测试数据。
     */
    @BeforeEach
    public void cleanupTestData() {
        this.rebuildSchemaIfOutdated();
        this.drainQueue();
        this.deleteConsumedHistoryByConsumerCode();
        this.deleteConsumedHistoryByMessageKey();
        this.deleteConsumedByConsumerCode();
        this.deleteConsumedByMessageKey();
        this.deleteSendLogByMessageKey();
        this.deleteMessageHistoryByMessageKey();
        this.deleteMessageByMessageKey();
    }

    /**
     * 验证消息发送到 RabbitMQ 后，消费者接口能消费消息并写入消费记录。
     */
    @Test
    public void shouldSendMessageAndRecordConsumedMessage() {
        String messageKey = "junit-" + System.currentTimeMillis();
        TransactionalMessage<Map<String, Object>> message = new TransactionalMessage<Map<String, Object>>()
                .setMessageKey(messageKey)
                .setProducerCode("spring-transactional-mq-demo-test")
                .setMqType(MqType.RABBITMQ)
                .setDestination(this.properties.getDestination())
                .setBizKey(messageKey)
                .setPayload(this.buildPayload(messageKey));

        SendResult sendResult = this.messagePublishService.publish(message);
        int dispatched = this.messageDispatchService.dispatchPendingMessages(1);
        Message rabbitMessage = this.rabbitTemplate.receive(this.properties.getQueueName(), 10000);

        Assertions.assertTrue(sendResult.isAccepted());
        Assertions.assertEquals(1, dispatched);
        Assertions.assertNotNull(rabbitMessage, "RabbitMQ should receive dispatched message");

        ConsumeContext context = this.buildConsumeContext(rabbitMessage, sendResult);
        Map<?, ?> payload = this.messagePayloadSerializer.deserialize(
                RabbitMqPayloadCodec.decode(rabbitMessage.getBody(),
                        rabbitMessage.getMessageProperties().getContentEncoding()),
                Map.class);
        DemoConsumer consumer = new DemoConsumer();

        boolean firstRecord = this.consumeIdempotentService.recordIfAbsent(context);
        this.rabbitMqConsumerInvoker.invoke(consumer, context, payload);
        boolean duplicatedRecord = this.consumeIdempotentService.recordIfAbsent(context);

        Assertions.assertTrue(firstRecord, "Consumed message should be recorded on first consume");
        Assertions.assertFalse(duplicatedRecord, "Duplicated consumed message should not be recorded again");
        Assertions.assertEquals(context.getId(), consumer.getConsumedId());
        Assertions.assertEquals(messageKey, payload.get("messageKey"));
        Assertions.assertEquals(MessageStatus.SUCCESS, this.findMessageStatus(context.getId()));
        Assertions.assertEquals(0, this.countMessageById(TRANSACTIONAL_MESSAGE_HISTORY_TABLE,
                TransactionalMessageHistoryEntity.class, context.getId()));
        Assertions.assertEquals(1, this.transactionalMessageCleanupService.cleanupSuccessMessages(
                new java.util.Date(System.currentTimeMillis() + 1000), 10));
        Assertions.assertEquals(0, this.countMessageById(TRANSACTIONAL_MESSAGE_TABLE,
                TransactionalMessageEntity.class, context.getId()));
        Assertions.assertEquals(1, this.countMessageById(TRANSACTIONAL_MESSAGE_HISTORY_TABLE,
                TransactionalMessageHistoryEntity.class, context.getId()));
        Assertions.assertEquals(1, this.countMessageById(MESSAGE_SEND_LOG_TABLE, MessageSendLogEntity.class,
                context.getId()));
        Assertions.assertEquals(1, this.countConsumed(CONSUMED_MESSAGE_TABLE, ConsumedMessageEntity.class, context));
        Assertions.assertEquals(1, this.countConsumed(CONSUMED_MESSAGE_HISTORY_TABLE,
                ConsumedMessageHistoryEntity.class, context));
    }

    private Map<String, Object> buildPayload(String messageKey) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("messageKey", messageKey);
        payload.put("source", "spring-transactional-mq-demo-junit");
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }

    private ConsumeContext buildConsumeContext(Message rabbitMessage, SendResult sendResult) {
        MessageProperties messageProperties = rabbitMessage.getMessageProperties();
        Object messageKey = messageProperties.getHeaders().get("messageKey");
        return new ConsumeContext()
                .setId(messageProperties.getMessageId())
                .setMessageKey(messageKey == null ? sendResult.getMessageKey() : String.valueOf(messageKey))
                .setConsumerCode(TEST_CONSUMER_CODE);
    }

    private void drainQueue() {
        Message message = this.rabbitTemplate.receive(this.properties.getQueueName(), 100);
        while (message != null) {
            message = this.rabbitTemplate.receive(this.properties.getQueueName(), 100);
        }
    }

    private void rebuildSchemaIfOutdated() {
        Integer oldColumnCount = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE COLUMN_NAME = 'MESSAGE_ID' "
                        + "AND TABLE_NAME IN ('TXN_MESSAGE', 'TXN_MESSAGE_HISTORY', 'TXN_CONSUMED_MESSAGE', "
                        + "'TXN_CONSUMED_MESSAGE_HISTORY', 'TXN_MESSAGE_SEND_LOG')",
                Integer.class);
        Integer dispatchTokenColumnCount = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'TXN_MESSAGE' "
                        + "AND COLUMN_NAME = 'DISPATCH_TOKEN'",
                Integer.class);
        if ((oldColumnCount == null || oldColumnCount < 1)
                && dispatchTokenColumnCount != null && dispatchTokenColumnCount > 0) {
            return;
        }
        this.dropTable("TXN_CONSUMED_MESSAGE_HISTORY");
        this.dropTable("TXN_CONSUMED_MESSAGE");
        this.dropTable("TXN_MESSAGE_SEND_LOG");
        this.dropTable("TXN_MESSAGE_HISTORY");
        this.dropTable("TXN_MESSAGE");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("sql/ORACLE.sql"));
        DatabasePopulatorUtils.execute(populator, this.jdbcTemplate.getDataSource());
    }

    private void dropTable(String tableName) {
        try {
            this.jdbcTemplate.execute("DROP TABLE " + tableName + " CASCADE CONSTRAINTS PURGE");
        } catch (DataAccessException ex) {
            // 表不存在时忽略，后续建表脚本会重新创建所需结构。
        }
    }

    private void deleteConsumedHistoryByConsumerCode() {
        this.ezDao.ezDelete(EzDelete.delete(CONSUMED_MESSAGE_HISTORY_TABLE)
                .where(w -> w.add(CONSUMED_MESSAGE_HISTORY_TABLE
                        .field(ConsumedMessageHistoryEntity.Fields.consumerCode).eq(TEST_CONSUMER_CODE)))
                .build());
    }

    private void deleteConsumedHistoryByMessageKey() {
        this.ezDao.ezDelete(EzDelete.delete(CONSUMED_MESSAGE_HISTORY_TABLE)
                .where(w -> w.add(CONSUMED_MESSAGE_HISTORY_TABLE
                        .field(ConsumedMessageHistoryEntity.Fields.messageKey).like(TEST_MESSAGE_KEY_PREFIX)))
                .build());
    }

    private void deleteConsumedByConsumerCode() {
        this.ezDao.ezDelete(EzDelete.delete(CONSUMED_MESSAGE_TABLE)
                .where(w -> w.add(CONSUMED_MESSAGE_TABLE.field(ConsumedMessageEntity.Fields.consumerCode)
                        .eq(TEST_CONSUMER_CODE)))
                .build());
    }

    private void deleteConsumedByMessageKey() {
        this.ezDao.ezDelete(EzDelete.delete(CONSUMED_MESSAGE_TABLE)
                .where(w -> w.add(CONSUMED_MESSAGE_TABLE.field(ConsumedMessageEntity.Fields.messageKey)
                        .like(TEST_MESSAGE_KEY_PREFIX)))
                .build());
    }

    private void deleteSendLogByMessageKey() {
        this.ezDao.ezDelete(EzDelete.delete(MESSAGE_SEND_LOG_TABLE)
                .where(w -> w.add(MESSAGE_SEND_LOG_TABLE.field(MessageSendLogEntity.Fields.messageKey)
                        .like(TEST_MESSAGE_KEY_PREFIX)))
                .build());
    }

    private void deleteMessageHistoryByMessageKey() {
        this.ezDao.ezDelete(EzDelete.delete(TRANSACTIONAL_MESSAGE_HISTORY_TABLE)
                .where(w -> w.add(TRANSACTIONAL_MESSAGE_HISTORY_TABLE
                        .field(TransactionalMessageEntity.Fields.messageKey).like(TEST_MESSAGE_KEY_PREFIX)))
                .build());
    }

    private void deleteMessageByMessageKey() {
        this.ezDao.ezDelete(EzDelete.delete(TRANSACTIONAL_MESSAGE_TABLE)
                .where(w -> w.add(TRANSACTIONAL_MESSAGE_TABLE.field(TransactionalMessageEntity.Fields.messageKey)
                        .like(TEST_MESSAGE_KEY_PREFIX)))
                .build());
    }

    private <Et> int countMessageById(EntityTable table, Class<Et> entityType, String id) {
        EzQuery<Et> query = EzQuery.builder(entityType)
                .from(table)
                .where(w -> w.add(table.field(BaseEntity.Fields.id).eq(id)))
                .build();
        return this.ezDao.queryCount(query);
    }

    private <Et> int countConsumed(EntityTable table, Class<Et> entityType, ConsumeContext context) {
        EzQuery<Et> query = EzQuery.builder(entityType)
                .from(table)
                .where(w -> w
                        .add(table.field(BaseEntity.Fields.id).eq(context.getId()))
                        .add(table.field(ConsumedMessageEntity.Fields.consumerCode).eq(context.getConsumerCode())))
                .build();
        return this.ezDao.queryCount(query);
    }

    private MessageStatus findMessageStatus(String id) {
        EzQuery<TransactionalMessageEntity> query = EzQuery.builder(TransactionalMessageEntity.class)
                .from(TRANSACTIONAL_MESSAGE_TABLE)
                .select(Select.EzSelectBuilder::addAll)
                .where(w -> w.add(TRANSACTIONAL_MESSAGE_TABLE.field(BaseEntity.Fields.id).eq(id)))
                .build();
        TransactionalMessageEntity entity = this.ezDao.queryOne(query);
        return entity == null ? null : entity.getMessageStatus();
    }

    private static final class DemoConsumer implements TransactionalMessageConsumer<Map<?, ?>> {

        private String consumedId;

        @Override
        public String getQueueName() {
            return "demo.queue";
        }

        @Override
        public String consumerCode() {
            return TEST_CONSUMER_CODE;
        }

        @Override
        public void consume(ConsumeContext context, Map<?, ?> payload) {
            Assertions.assertNotNull(payload);
            this.consumedId = context.getId();
        }

        private String getConsumedId() {
            return consumedId;
        }
    }
}
