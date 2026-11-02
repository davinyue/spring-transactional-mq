package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.transactionalmq.api.model.ConsumeContext;
import org.rdlinux.transactionalmq.common.entity.BaseEntity;
import org.rdlinux.transactionalmq.core.repository.ConsumedMessageRepository;
import org.rdlinux.transactionalmq.core.repository.MessageSendLogRepository;
import org.rdlinux.transactionalmq.core.model.MessageSendLogRecord;
import org.rdlinux.transactionalmq.core.service.ConsumeIdempotentService;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.ConsumedMessageHistoryEntity;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collections;

/**
 * 消费记录保存点和业务事务原子性测试。
 */
public class ConsumedMessageTransactionTest {
    /**
     * 验证发送日志冲突回滚至保存点，成功的日志随外层事务回滚。
     *
     * @throws Exception 初始化测试仓储失败
     */
    @Test
    public void duplicateSendLogDoesNotPoisonOuterTransaction() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:send_tx;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE send_log (id VARCHAR(24) PRIMARY KEY)");
        jdbc.execute("CREATE TABLE business (id VARCHAR(24) PRIMARY KEY)");
        jdbc.update("INSERT INTO send_log VALUES (?)", "message");
        EzDao dao = Mockito.mock(EzDao.class);
        Mockito.doAnswer(invocation -> {
            BaseEntity<?> entity = invocation.getArgument(0);
            jdbc.update("INSERT INTO send_log VALUES (?)", entity.getId());
            return null;
        }).when(dao).insert(Mockito.any());
        EzMybatisMessageSendLogRepository target = new EzMybatisMessageSendLogRepository();
        Field field = EzMybatisMessageSendLogRepository.class.getDeclaredField("ezDao");
        field.setAccessible(true);
        field.set(target, dao);
        DataSourceTransactionManager manager = new DataSourceTransactionManager(source);
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        MessageSendLogRepository repository = (MessageSendLogRepository) factory.getProxy();
        new TransactionTemplate(manager).execute(status -> {
            MessageSendLogRecord record = new MessageSendLogRecord();
            record.setId("message");
            try {
                repository.save(record);
                Assert.fail("重复日志应抛出异常");
            } catch (DataIntegrityViolationException expected) {
                jdbc.update("INSERT INTO business VALUES (?)", "outer-work");
            }
            return null;
        });
        Assert.assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM business", Integer.class));
        new TransactionTemplate(manager).execute(status -> {
            MessageSendLogRecord record = new MessageSendLogRecord();
            record.setId("rolled-back");
            repository.save(record);
            Assert.assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                    "SELECT COUNT(*) FROM send_log WHERE id='rolled-back'", Integer.class));
            status.setRollbackOnly();
            return null;
        });
        Assert.assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM send_log WHERE id='rolled-back'", Integer.class));
        MessageSendLogRecord standalone = new MessageSendLogRecord();
        standalone.setId("standalone");
        repository.save(standalone);
        Assert.assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM send_log WHERE id='standalone'", Integer.class));
    }

    /**
     * 验证历史表冲突回滚主表插入，外层事务仍可提交；业务回滚时新记录也随之回滚。
     *
     * @throws Exception 初始化测试仓储失败
     */
    @Test
    public void duplicateHistoryRollsBackSavepointAndBusinessRollbackRemovesRecords() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:consume_tx;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE consumed (id VARCHAR(24) PRIMARY KEY)");
        jdbc.execute("CREATE TABLE history (id VARCHAR(24) PRIMARY KEY)");
        jdbc.update("INSERT INTO history VALUES (?)", "original");
        EzDao dao = Mockito.mock(EzDao.class);
        Mockito.doAnswer(invocation -> {
            BaseEntity<?> entity = invocation.getArgument(0);
            String table = entity instanceof ConsumedMessageEntity ? "consumed" : "history";
            try {
                jdbc.update("INSERT INTO " + table + " VALUES (?)", entity.getId());
            } catch (DataIntegrityViolationException ex) {
                throw new DataIntegrityViolationException("GaussDB duplicate", new SQLException("duplicate", "23505"));
            }
            return null;
        }).when(dao).insert(Mockito.any());
        Mockito.when(dao.query(Mockito.any())).thenReturn(Collections.emptyList(),
                Collections.singletonList(new ConsumedMessageHistoryEntity()));
        EzMybatisConsumedMessageRepository target = new EzMybatisConsumedMessageRepository();
        Field field = EzMybatisConsumedMessageRepository.class.getDeclaredField("ezDao");
        field.setAccessible(true);
        field.set(target, dao);
        DataSourceTransactionManager manager = new DataSourceTransactionManager(source);
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        ConsumeIdempotentService service = new ConsumeIdempotentService((ConsumedMessageRepository) factory.getProxy());
        TransactionTemplate transaction = new TransactionTemplate(manager);

        transaction.execute(status -> {
            Assert.assertFalse(service.recordIfAbsent(new ConsumeContext().setId("original").setConsumerCode("c")));
            Assert.assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT COUNT(*) FROM consumed", Integer.class));
            jdbc.update("INSERT INTO consumed VALUES (?)", "outer-work");
            return null;
        });
        Assert.assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM consumed", Integer.class));
        transaction.execute(status -> {
            Assert.assertTrue(service.recordIfAbsent(new ConsumeContext().setId("new").setConsumerCode("c")));
            status.setRollbackOnly();
            return null;
        });
        Assert.assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT COUNT(*) FROM consumed WHERE id='new'", Integer.class));
        Assert.assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT COUNT(*) FROM history WHERE id='new'", Integer.class));
    }
}
