package org.rdlinux.transactionalmq.starter.config;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.ezmybatis.constant.DbType;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.ezmybatis.core.mapper.EzMapper;
import org.rdlinux.ezmybatis.core.sqlstruct.table.DbTable;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.mq.MqProducerRouter;
import org.rdlinux.transactionalmq.kafka.KafkaProducerAdapter;
import org.rdlinux.transactionalmq.core.serialize.LuavaJsonMessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumedMessageCleanupService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqProducerAdapter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * starter 自动装配测试
 */
public class TransactionalMqAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TransactionalMqAutoConfiguration.class,
                    TransactionalMqRabbitAutoConfiguration.class,
                    TransactionalMqKafkaAutoConfiguration.class));

    @Test
    public void should_register_message_publish_service_when_enabled() {
        this.contextRunner
                .withPropertyValues(TransactionalMqProperties.PREFIX + ".enabled=true",
                        TransactionalMqProperties.PREFIX + ".dispatch-batch-size=23",
                        TransactionalMqProperties.PREFIX + ".consume-record-retention-days=8")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
                    Assert.assertTrue(context.containsBean("messagePublishService"));
                    Assert.assertNotNull(context.getBean(MessagePublishService.class));
                    Assert.assertNotNull(context.getBean(MqProducerRouter.class));
                    Assert.assertTrue(context.containsBean("transactionalMessageRepository"));
                    Assert.assertTrue(context.containsBean("consumedMessageRepository"));
                    Assert.assertTrue(context.containsBean("messageSendLogRepository"));
                    Assert.assertTrue(context.containsBean("consumedMessageCleanupService"));
                    Assert.assertTrue(context.containsBean("consumedMessageCleanupScheduler"));
                    Assert.assertTrue(context.containsBean("transactionalMessageCleanupService"));
                    Assert.assertTrue(context.containsBean("transactionalMessageCleanupScheduler"));
                    Assert.assertNotNull(context.getBean(ConsumedMessageCleanupService.class));
                    TransactionalMqProperties properties = context.getBean(TransactionalMqProperties.class);
                    Assert.assertTrue(properties.isEnabled());
                    Assert.assertEquals(23, properties.getDispatchBatchSize());
                    Assert.assertEquals(8, properties.getConsumeRecordRetentionDays());
                });
    }

    @Test
    public void should_enable_auto_init_schema_by_default() {
        this.contextRunner
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
                    TransactionalMqProperties properties = context.getBean(TransactionalMqProperties.class);
                    Assert.assertTrue(properties.isAutoInitSchema());
                });
    }

    @Test
    public void should_register_default_message_payload_serializer_and_round_trip_payload() {
        this.contextRunner
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
            MessagePayloadSerializer serializer = context.getBean(MessagePayloadSerializer.class);
            Assert.assertTrue(serializer instanceof LuavaJsonMessagePayloadSerializer);
            SamplePayload payload = new SamplePayload("demo", 3);
            String serialized = serializer.serialize(payload);
            SamplePayload deserialized = serializer.deserialize(serialized, SamplePayload.class);
            Assert.assertEquals(payload, deserialized);
        });
    }

    @Test
    public void should_use_user_defined_message_payload_serializer_when_present() {
        this.contextRunner
                .withBean(MessagePayloadSerializer.class, CustomMessagePayloadSerializer::new)
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
                    MessagePayloadSerializer serializer = context.getBean(MessagePayloadSerializer.class);
                    Assert.assertTrue(serializer instanceof CustomMessagePayloadSerializer);
                    Assert.assertEquals("custom:demo", serializer.serialize("demo"));
                    Assert.assertEquals(1, context.getBeansOfType(MessagePayloadSerializer.class).size());
                });
    }

    @Test
    public void should_not_register_auto_configuration_when_disabled() {
        this.contextRunner
                .withPropertyValues(TransactionalMqProperties.PREFIX + ".enabled=false")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
                    Assert.assertFalse(context.containsBean("messagePublishService"));
                    Assert.assertFalse(context.containsBean("rabbitMqProducerAdapter"));
                    Assert.assertFalse(context.containsBean("transactionalMessageRepository"));
                    Assert.assertFalse(context.containsBean("transactionalMqStartupValidator"));
                });
    }

    @Test
    public void should_register_rabbit_mq_producer_adapter_when_rabbit_template_present() {
        this.contextRunner
                .withPropertyValues(TransactionalMqProperties.PREFIX + ".enabled=true")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
                    Assert.assertTrue(context.containsBean("rabbitMqProducerAdapter"));
                    Assert.assertTrue(context.getBean(RabbitMqProducerAdapter.class) instanceof RabbitMqProducerAdapter);
                    Assert.assertNotNull(context.getBean(MqProducerRouter.class));
                    Assert.assertTrue(context.containsBean("transactionalMessageRepository"));
                });
    }

    @Test
    public void should_register_kafka_producer_adapter_when_kafka_template_present() {
        this.contextRunner
                .withPropertyValues(TransactionalMqProperties.PREFIX + ".enabled=true")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                .run(context -> {
                    Assert.assertTrue(context.containsBean("kafkaProducerAdapter"));
                    Assert.assertTrue(context.getBean(KafkaProducerAdapter.class) instanceof KafkaProducerAdapter);
                    Assert.assertNotNull(context.getBean(MqProducerRouter.class));
                });
    }

    @Test
    public void should_register_rabbit_and_kafka_adapters_together() {
        this.contextRunner
                .withPropertyValues(TransactionalMqProperties.PREFIX + ".enabled=true")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                .withBean(ConsumerFactory.class, () -> mock(ConsumerFactory.class))
                .run(context -> {
                    Assert.assertTrue(context.containsBean("rabbitMqProducerAdapter"));
                    Assert.assertTrue(context.containsBean("kafkaProducerAdapter"));
                    Assert.assertEquals(2, context.getBeansOfType(org.rdlinux.transactionalmq.core.mq.MqProducerAdapter.class).size());
                });
    }

    @Test
    public void should_fail_when_no_mq_implementation_found() {
        this.contextRunner
                .withPropertyValues(TransactionalMqProperties.PREFIX + ".enabled=true")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withClassLoader(new FilteredClassLoader(
                        "org.springframework.amqp.rabbit",
                        "org.rdlinux.transactionalmq.rabbitmq"))
                .run(context -> {
                    Assert.assertNotNull(context.getStartupFailure());
                    Assert.assertTrue(context.getStartupFailure().getMessage().contains("No MqProducerAdapter found"));
                });
    }

    @Test
    public void should_be_listed_in_spring_factories_as_auto_configuration() {
        String className = TransactionalMqAutoConfiguration.class.getName();
        String rabbitClassName = TransactionalMqRabbitAutoConfiguration.class.getName();
        String kafkaClassName = TransactionalMqKafkaAutoConfiguration.class.getName();
        Assert.assertTrue(SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
                this.getClass().getClassLoader()).contains(className));
        Assert.assertTrue(SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
                this.getClass().getClassLoader()).contains(rabbitClassName));
        Assert.assertTrue(SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
                this.getClass().getClassLoader()).contains(kafkaClassName));
    }

    @Test
    public void resolveScriptLocationShouldMatchMysql() {
        Assert.assertEquals("sql/MYSQL.sql",
                TransactionalMqSchemaInitializer.resolveScriptLocation(DbType.MYSQL));
    }

    @Test
    public void resolveScriptLocationShouldMatchOracle() {
        Assert.assertEquals("sql/ORACLE.sql",
                TransactionalMqSchemaInitializer.resolveScriptLocation(DbType.ORACLE));
    }

    @Test
    public void resolveScriptLocationShouldMatchDm() {
        Assert.assertEquals("sql/DM.sql",
                TransactionalMqSchemaInitializer.resolveScriptLocation(DbType.DM));
    }

    @Test
    public void resolveScriptLocationShouldMatchPostgreSql() {
        Assert.assertEquals("sql/POSTGRE_SQL.sql",
                TransactionalMqSchemaInitializer.resolveScriptLocation(DbType.POSTGRE_SQL));
    }

    @Test
    public void resolveScriptLocationShouldMatchSqlServer() {
        Assert.assertEquals("sql/SQL_SERVER.sql",
                TransactionalMqSchemaInitializer.resolveScriptLocation(DbType.SQL_SERVER));
    }

    @Test
    public void should_register_schema_initializer_when_auto_init_schema_enabled() {
        org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory = mock(org.apache.ibatis.session.SqlSessionFactory.class);
        when(sqlSessionFactory.getConfiguration()).thenReturn(new org.apache.ibatis.session.Configuration());
        this.contextRunner
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(EzMapper.class, () -> mock(EzMapper.class))
                .withBean("dataSource", DataSource.class, () -> new NoOpDataSource())
                .withBean(org.apache.ibatis.session.SqlSessionFactory.class, () -> sqlSessionFactory)
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> Assert.assertTrue(context.containsBean("transactionalMqSchemaInitializer")));
    }

    @Test
    public void should_not_register_schema_initializer_when_auto_init_schema_disabled() {
        org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory = mock(org.apache.ibatis.session.SqlSessionFactory.class);
        when(sqlSessionFactory.getConfiguration()).thenReturn(new org.apache.ibatis.session.Configuration());
        this.contextRunner
                .withPropertyValues(TransactionalMqProperties.PREFIX + ".auto-init-schema=false")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(EzMapper.class, () -> mock(EzMapper.class))
                .withBean("dataSource", DataSource.class, () -> new NoOpDataSource())
                .withBean(org.apache.ibatis.session.SqlSessionFactory.class, () -> sqlSessionFactory)
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> Assert.assertFalse(context.containsBean("transactionalMqSchemaInitializer")));
    }

    @Test
    public void schemaInitializerShouldSwallowExecutionException() throws Exception {
        EzMapper ezMapper = mock(EzMapper.class);
        TransactionalMqSchemaInitializer initializer = new TransactionalMqSchemaInitializer(new BrokenDataSource(),
                new org.apache.ibatis.session.Configuration(), ezMapper) {
            @Override
            protected DbType resolveDbType() {
                return DbType.MYSQL;
            }

            @Override
            protected void executeScript(String scriptLocation) {
                throw new IllegalStateException("broken");
            }
        };

        initializer.afterPropertiesSet();
    }

    @Test
    public void schemaInitializerShouldNotExecuteScriptWhenAllTablesExist() throws Exception {
        EzMapper ezMapper = mock(EzMapper.class);
        when(ezMapper.tableExists(org.mockito.ArgumentMatchers.any(DbTable.class))).thenReturn(true);
        AtomicBoolean executed = new AtomicBoolean(false);
        TransactionalMqSchemaInitializer initializer = new TransactionalMqSchemaInitializer(new NoOpDataSource(),
                new org.apache.ibatis.session.Configuration(), ezMapper) {
            @Override
            protected DbType resolveDbType() {
                return DbType.MYSQL;
            }

            @Override
            protected void executeScript(String scriptLocation) {
                executed.set(true);
            }
        };

        initializer.afterPropertiesSet();

        Assert.assertFalse(executed.get());
    }

    @Test
    public void schemaInitializerShouldExecuteScriptWhenAnyTableMissing() throws Exception {
        EzMapper ezMapper = mock(EzMapper.class);
        when(ezMapper.tableExists(org.mockito.ArgumentMatchers.any(DbTable.class)))
                .thenReturn(true, true, false);
        AtomicBoolean executed = new AtomicBoolean(false);
        TransactionalMqSchemaInitializer initializer = new TransactionalMqSchemaInitializer(new NoOpDataSource(),
                new org.apache.ibatis.session.Configuration(), ezMapper) {
            @Override
            protected DbType resolveDbType() {
                return DbType.MYSQL;
            }

            @Override
            protected void executeScript(String scriptLocation) {
                executed.set(true);
            }
        };

        initializer.afterPropertiesSet();

        Assert.assertTrue(executed.get());
    }

    private static final class SamplePayload {

        private String name;
        private int count;

        private SamplePayload() {
        }

        private SamplePayload(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return this.count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SamplePayload)) {
                return false;
            }
            SamplePayload other = (SamplePayload) obj;
            return this.count == other.count && (this.name == null ? other.name == null : this.name.equals(other.name));
        }

        @Override
        public int hashCode() {
            int result = this.name != null ? this.name.hashCode() : 0;
            result = 31 * result + this.count;
            return result;
        }
    }

    private static final class CustomMessagePayloadSerializer implements MessagePayloadSerializer {

        @Override
        public String serialize(Object payload) {
            return "custom:" + payload;
        }

        @Override
        public <T> T deserialize(String payloadText, Type targetType) {
            return null;
        }
    }

    private static final class NoOpDataSource extends AbstractDataSource {

        @Override
        public Connection getConnection() throws SQLException {
            return mock(Connection.class);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return this.getConnection();
        }
    }

    private static final class BrokenDataSource extends AbstractDataSource {

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("broken");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("broken");
        }
    }
}
