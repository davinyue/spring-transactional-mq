package org.rdlinux.transactionalmq.starter.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.rdlinux.ezmybatis.core.EzDelete;
import org.rdlinux.ezmybatis.core.EzQuery;
import org.rdlinux.ezmybatis.core.EzUpdate;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.mapper.EzMapper;
import org.rdlinux.transactionalmq.api.model.SendResult;
import org.rdlinux.transactionalmq.api.model.TransactionalMessage;
import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.service.MessageDispatchService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.core.service.TransactionalMessageCleanupService;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageHistoryEntity;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * starter 主链路集成回归测试。
 */
public class TransactionalMqMainChainIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TransactionalMqAutoConfiguration.class));

    @Test
    public void should_wire_publish_and_dispatch_main_chain() {
        StatefulEzDao ezDao = new StatefulEzDao();
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        this.contextRunner
            .withPropertyValues("transactional.mq.enabled=true")
            .withBean(EzDao.class, () -> ezDao)
            .withBean(RabbitTemplate.class, () -> rabbitTemplate)
            .run(context -> {
                assertTrue(context.containsBean("transactionalMessageRepository"));
                assertTrue(context.containsBean("messagePublishService"));
                assertTrue(context.containsBean("messageDispatchService"));
                assertTrue(context.containsBean("transactionalMessageCleanupService"));
                assertTrue(context.containsBean("rabbitMqProducerAdapter"));

                MessagePublishService publishService = context.getBean(MessagePublishService.class);
                MessageDispatchService dispatchService = context.getBean(MessageDispatchService.class);
                TransactionalMessageCleanupService cleanupService =
                    context.getBean(TransactionalMessageCleanupService.class);

                TransactionalMessage<String> message = new TransactionalMessage<String>()
                    .setMessageKey("message-key-8")
                    .setProducerCode("producer-8")
                    .setMqType(MqType.RABBITMQ)
                    .setDestination("exchange.demo:queue.demo")
                    .setPayload("payload-8")
                    .setBizKey("biz-8");

                SendResult sendResult = publishService.publish(message);

                assertTrue(sendResult.isAccepted());
                assertNotNull(sendResult.getId());
                assertEquals("message-key-8", sendResult.getMessageKey());
                assertEquals(1, ezDao.getTransactionalMessageCount());
                assertEquals(sendResult.getId(), ezDao.getTransactionalMessage(0).getId());
                assertEquals(MessageStatus.INIT, ezDao.getTransactionalMessage(0).getMessageStatus());
                assertEquals(0, ezDao.getTransactionalMessageHistoryCount());

                int dispatched = dispatchService.dispatchPendingMessages(10);

                assertEquals(1, dispatched);
                assertEquals(MessageStatus.SUCCESS, ezDao.getTransactionalMessage(0).getMessageStatus());
                verify(rabbitTemplate).convertAndSend(eq("exchange.demo"), eq("queue.demo"), any(Object.class),
                    any(MessagePostProcessor.class));

                int deleted = cleanupService.cleanupSuccessMessages(new Date(System.currentTimeMillis() + 1000), 10);

                assertEquals(1, deleted);
                assertEquals(0, ezDao.getTransactionalMessageCount());
                assertEquals(1, ezDao.getTransactionalMessageHistoryCount());
                assertEquals(sendResult.getId(), ezDao.getTransactionalMessageHistory(0).getId());
            });
    }

    private static final class StatefulEzDao extends EzDao {

        private final List<TransactionalMessageEntity> transactionalMessageEntities =
            new ArrayList<TransactionalMessageEntity>();
        private final List<TransactionalMessageHistoryEntity> transactionalMessageHistoryEntities =
            new ArrayList<TransactionalMessageHistoryEntity>();

        private StatefulEzDao() {
            super(mock(EzMapper.class));
        }

        @Override
        public int insert(Object model) {
            if (model instanceof TransactionalMessageHistoryEntity) {
                TransactionalMessageHistoryEntity entity = copy((TransactionalMessageHistoryEntity) model);
                if (entity.getId() == null || entity.getId().isEmpty()) {
                    entity.setId(generateId());
                }
                this.transactionalMessageHistoryEntities.add(entity);
            } else if (model instanceof TransactionalMessageEntity) {
                TransactionalMessageEntity entity = copy((TransactionalMessageEntity) model);
                if (entity.getId() == null || entity.getId().isEmpty()) {
                    entity.setId(generateId());
                }
                this.transactionalMessageEntities.add(entity);
            }
            return 1;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <Rt> List<Rt> query(EzQuery<Rt> query) {
            List<Rt> result = new ArrayList<Rt>();
            Date now = new Date();
            for (TransactionalMessageEntity entity : this.transactionalMessageEntities) {
                if (entity.getMessageStatus() == MessageStatus.INIT
                    && entity.getNextDispatchTime() != null
                    && !entity.getNextDispatchTime().after(now)) {
                    result.add((Rt) copy(entity));
                } else if (entity.getMessageStatus() == MessageStatus.SUCCESS
                    && entity.getUpdateTime() != null
                    && !entity.getUpdateTime().after(now)) {
                    result.add((Rt) copy(entity));
                }
            }
            return result;
        }

        @Override
        public int ezUpdate(EzUpdate update) {
            Date now = new Date();
            for (TransactionalMessageEntity entity : this.transactionalMessageEntities) {
                if (entity.getMessageStatus() == MessageStatus.INIT
                    && entity.getNextDispatchTime() != null
                    && !entity.getNextDispatchTime().after(now)) {
                    entity.setMessageStatus(MessageStatus.SENDING);
                    entity.setUpdateTime(now);
                    entity.setNextDispatchTime(new Date(now.getTime() + 5 * 60 * 1000L));
                    return 1;
                }
                if (entity.getMessageStatus() == MessageStatus.SENDING) {
                    entity.setMessageStatus(MessageStatus.SUCCESS);
                    entity.setUpdateTime(now);
                    entity.setNextDispatchTime(null);
                    return 1;
                }
                if (entity.getMessageStatus() == MessageStatus.SUCCESS
                    && entity.getUpdateTime() != null
                    && !entity.getUpdateTime().after(now)) {
                    entity.setMessageStatus(MessageStatus.ARCHIVING);
                    entity.setUpdateTime(now);
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public int ezDelete(EzDelete delete) {
            int count = 0;
            for (int i = this.transactionalMessageEntities.size() - 1; i >= 0; i--) {
                TransactionalMessageEntity entity = this.transactionalMessageEntities.get(i);
                if (entity.getMessageStatus() == MessageStatus.ARCHIVING) {
                    this.transactionalMessageEntities.remove(i);
                    count++;
                }
            }
            return count;
        }

        private int getTransactionalMessageCount() {
            return this.transactionalMessageEntities.size();
        }

        private TransactionalMessageEntity getTransactionalMessage(int index) {
            return this.transactionalMessageEntities.get(index);
        }

        private int getTransactionalMessageHistoryCount() {
            return this.transactionalMessageHistoryEntities.size();
        }

        private TransactionalMessageHistoryEntity getTransactionalMessageHistory(int index) {
            return this.transactionalMessageHistoryEntities.get(index);
        }

        private TransactionalMessageEntity copy(TransactionalMessageEntity source) {
            TransactionalMessageEntity target = new TransactionalMessageEntity();
            target.setId(source.getId());
            target.setCreateTime(copyDate(source.getCreateTime()));
            target.setUpdateTime(copyDate(source.getUpdateTime()));
            target.setMessageKey(source.getMessageKey());
            target.setProducerCode(source.getProducerCode());
            target.setMqType(source.getMqType());
            target.setDestination(source.getDestination());
            target.setRoute(source.getRoute());
            target.setShardingKey(source.getShardingKey());
            target.setPayloadText(source.getPayloadText());
            target.setHeadersJson(source.getHeadersJson());
            target.setBizKey(source.getBizKey());
            target.setMessageStatus(source.getMessageStatus());
            target.setNextDispatchTime(copyDate(source.getNextDispatchTime()));
            return target;
        }

        private TransactionalMessageHistoryEntity copy(TransactionalMessageHistoryEntity source) {
            TransactionalMessageHistoryEntity target = new TransactionalMessageHistoryEntity();
            target.setId(source.getId());
            target.setCreateTime(copyDate(source.getCreateTime()));
            target.setUpdateTime(copyDate(source.getUpdateTime()));
            target.setMessageKey(source.getMessageKey());
            target.setProducerCode(source.getProducerCode());
            target.setMqType(source.getMqType());
            target.setDestination(source.getDestination());
            target.setRoute(source.getRoute());
            target.setShardingKey(source.getShardingKey());
            target.setPayloadText(source.getPayloadText());
            target.setHeadersJson(source.getHeadersJson());
            target.setBizKey(source.getBizKey());
            target.setMessageStatus(source.getMessageStatus());
            target.setNextDispatchTime(copyDate(source.getNextDispatchTime()));
            return target;
        }

        private Date copyDate(Date date) {
            if (date == null) {
                return null;
            }
            return new Date(date.getTime());
        }

        private String generateId() {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
