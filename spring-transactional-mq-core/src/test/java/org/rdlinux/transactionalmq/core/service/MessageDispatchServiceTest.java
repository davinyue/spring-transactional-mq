package org.rdlinux.transactionalmq.core.service;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.common.enums.SendStatus;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;
import org.rdlinux.transactionalmq.core.model.MessageSendLogRecord;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.core.mq.MqProducerAdapter;
import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository;
import org.rdlinux.transactionalmq.core.repository.TransactionalMessageRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 消息派发服务测试
 */
public class MessageDispatchServiceTest {

    /** 验证 GaussDB 重复键不阻断派发，其他完整性错误仍然抛出。 */
    @Test
    public void dispatchShouldOnlyIgnoreDuplicateSqlState() {
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository(buildRecord("msg"));
        FailingMessageSendLogRepository logs = new FailingMessageSendLogRepository();
        logs.failure = new DataIntegrityViolationException("duplicate", new SQLException("duplicate", "23505"));
        MessageDispatchService service = new MessageDispatchService(repository,
                new MqProducerRouter(java.util.Collections.<MqProducerAdapter>singletonList(
                        new CapturingMqProducerAdapter())), logs);
        Assert.assertEquals(1, service.dispatchPendingMessages(1));
        Assert.assertEquals(1, repository.successRecords.size());
        logs.failure = new DataIntegrityViolationException("other", new SQLException("not null", "23502"));
        try {
            service.dispatchPendingMessages(1);
            Assert.fail("其他完整性错误必须抛出");
        } catch (DataIntegrityViolationException ex) {
            Assert.assertSame(logs.failure, ex);
        }
    }

    @Test
    public void dispatchPendingMessagesShouldSendEachMessageToAdapter() {
        TransactionalMessageRecord skippedRecord = buildRecord("msg-1");
        TransactionalMessageRecord successRecord = buildRecord("msg-2");
        TransactionalMessageRecord failedRecord = buildRecord("msg-3");

        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository(failedRecord,
                skippedRecord, successRecord);
        repository.skippedId = "msg-1";
        CapturingMqProducerAdapter adapter = new CapturingMqProducerAdapter();
        adapter.failedId = "msg-3";
        CapturingMessageSendLogRepository logRepository = new CapturingMessageSendLogRepository();
        MessageDispatchService service = new MessageDispatchService(repository,
                new MqProducerRouter(java.util.Collections.<MqProducerAdapter>singletonList(adapter)), logRepository);

        int dispatched = service.dispatchPendingMessages(10);

        Assert.assertEquals(2, dispatched);
        Assert.assertEquals(1, repository.findCalls);
        Assert.assertEquals(10, repository.claimLimit);
        Assert.assertEquals(Arrays.asList("msg-1", "msg-2", "msg-3"), repository.claimedIds);
        Assert.assertEquals(2, adapter.sentMessages.size());
        Assert.assertEquals("msg-2", adapter.sentMessages.get(0).getId());
        Assert.assertEquals("msg-3", adapter.sentMessages.get(1).getId());
        Assert.assertEquals("demo.exchange", adapter.sentMessages.get(0).getDestination());
        Assert.assertEquals("demo.route", adapter.sentMessages.get(0).getRoute());
        Assert.assertEquals("order-1", adapter.sentMessages.get(0).getShardingKey());
        Assert.assertEquals("payload-value", adapter.sentMessages.get(0).getPayloadText());
        Assert.assertEquals(2, logRepository.savedLogs.size());
        Assert.assertEquals("msg-2", logRepository.savedLogs.get(0).getId());
        Assert.assertEquals(SendStatus.SUCCESS, logRepository.savedLogs.get(0).getSendStatus());
        Assert.assertNotNull(logRepository.savedLogs.get(0).getLastSendTime());
        Assert.assertEquals(1, repository.successRecords.size());
        Assert.assertEquals("msg-2", repository.successRecords.get(0).getId());
        Assert.assertEquals(1, repository.failedRecords.size());
        Assert.assertEquals("msg-3", repository.failedRecords.get(0).getId());
    }

    @Test
    public void dispatchPendingMessagesShouldStillMarkResultWhenSaveLogFails() {
        TransactionalMessageRecord successRecord = buildRecord("msg-11");

        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository(successRecord);
        CapturingMqProducerAdapter adapter = new CapturingMqProducerAdapter();
        FailingMessageSendLogRepository logRepository = new FailingMessageSendLogRepository();
        MessageDispatchService service = new MessageDispatchService(repository,
                new MqProducerRouter(java.util.Collections.<MqProducerAdapter>singletonList(adapter)), logRepository);

        int dispatched = service.dispatchPendingMessages(1);

        Assert.assertEquals(1, dispatched);
        Assert.assertEquals(1, repository.successRecords.size());
        Assert.assertEquals("msg-11", repository.successRecords.get(0).getId());
        Assert.assertTrue(repository.failedRecords.isEmpty());
    }

    /**
     * 验证派发服务从仓储获取最近的未来派发时间。
     */
    @Test
    public void findNextDispatchTimeShouldDelegateToRepository() {
        Date expectedTime = new Date(System.currentTimeMillis() + 5000L);
        CapturingTransactionalMessageRepository repository = new CapturingTransactionalMessageRepository();
        repository.nextDispatchTime = expectedTime;
        MessageDispatchService service = new MessageDispatchService(repository,
                new MqProducerRouter(java.util.Collections.<MqProducerAdapter>singletonList(
                        new CapturingMqProducerAdapter())));

        Date actualTime = service.findNextDispatchTime();

        Assert.assertSame(expectedTime, actualTime);
        Assert.assertEquals(1, repository.nextDispatchTimeCalls);
    }

    private static TransactionalMessageRecord buildRecord(String id) {
        TransactionalMessageRecord record = new TransactionalMessageRecord();
        record.setId(id);
        record.setMessageKey("message-key-1");
        record.setProducerCode("producer-1");
        record.setMqType(MqType.RABBITMQ);
        record.setDestination("demo.exchange");
        record.setRoute("demo.route");
        record.setShardingKey("order-1");
        record.setPayloadText("payload-value");
        record.setBizKey("biz-1");
        return record;
    }

    private static class CapturingTransactionalMessageRepository implements TransactionalMessageRepository {

        private final List<TransactionalMessageRecord> records;
        private int findCalls;
        private int claimLimit;
        private String skippedId;
        /**
         * 模拟仓储返回的最近派发时间。
         */
        private Date nextDispatchTime;
        /**
         * 最近派发时间查询次数。
         */
        private int nextDispatchTimeCalls;
        private final List<String> claimedIds = new ArrayList<String>();
        private final List<TransactionalMessageRecord> successRecords = new ArrayList<TransactionalMessageRecord>();
        private final List<TransactionalMessageRecord> failedRecords = new ArrayList<TransactionalMessageRecord>();

        private CapturingTransactionalMessageRepository(TransactionalMessageRecord... records) {
            this.records = Arrays.asList(records);
        }

        @Override
        public TransactionalMessageRecord save(TransactionalMessageRecord record) {
            return record;
        }

        @Override
        public List<TransactionalMessageRecord> findDispatchCandidates(int limit) {
            this.findCalls++;
            this.claimLimit = limit;
            return this.records;
        }

        @Override
        public Date findNextDispatchTime() {
            this.nextDispatchTimeCalls++;
            return this.nextDispatchTime;
        }

        @Override
        public TransactionalMessageRecord claimDispatchMessage(TransactionalMessageRecord record) {
            this.claimedIds.add(record.getId());
            if (record.getId().equals(this.skippedId)) {
                return null;
            }
            record.setDispatchToken("token-" + record.getId());
            return record;
        }

        @Override
        public void markDispatchSuccess(List<TransactionalMessageRecord> records) {
            this.successRecords.addAll(records);
        }

        @Override
        public void markDispatchFailed(List<TransactionalMessageRecord> records) {
            this.failedRecords.addAll(records);
        }

        @Override
        public List<TransactionalMessageRecord> findSuccessCleanupCandidates(Date cleanupBefore, int limit) {
            return java.util.Collections.emptyList();
        }

        @Override
        public int archiveSuccessMessage(TransactionalMessageRecord record, Date cleanupBefore) {
            return 0;
        }
    }

    private static class CapturingMqProducerAdapter implements MqProducerAdapter {

        private final List<DispatchMessage> sentMessages = new ArrayList<DispatchMessage>();
        private String failedId;

        @Override
        public MqType supportMqType() {
            return MqType.RABBITMQ;
        }

        @Override
        public void send(DispatchMessage message) {
            this.sentMessages.add(message);
            if (message.getId().equals(this.failedId)) {
                throw new RuntimeException("send failed");
            }
        }
    }

    private static class CapturingMessageSendLogRepository implements MessageSendLogRepository {

        private final List<MessageSendLogRecord> savedLogs = new ArrayList<MessageSendLogRecord>();

        @Override
        public MessageSendLogRecord save(MessageSendLogRecord record) {
            this.savedLogs.add(record);
            return record;
        }

        @Override
        public List<MessageSendLogRecord> findRetryCandidates(int limit) {
            return new ArrayList<MessageSendLogRecord>();
        }
    }

    private static class FailingMessageSendLogRepository implements MessageSendLogRepository {

        /** 模拟的发送日志异常。 */
        private DataIntegrityViolationException failure = new DuplicateKeyException("duplicate");

        @Override
        public MessageSendLogRecord save(MessageSendLogRecord record) {
            throw this.failure;
        }

        @Override
        public List<MessageSendLogRecord> findRetryCandidates(int limit) {
            return new ArrayList<MessageSendLogRecord>();
        }
    }
}
