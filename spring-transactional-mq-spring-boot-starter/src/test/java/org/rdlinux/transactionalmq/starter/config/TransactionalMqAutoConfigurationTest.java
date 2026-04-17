package org.rdlinux.transactionalmq.starter.config;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.ezmybatis.core.dao.EzDao;
import org.rdlinux.transactionalmq.api.serialize.MessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.serialize.LuavaJsonMessagePayloadSerializer;
import org.rdlinux.transactionalmq.core.service.ConsumedMessageCleanupService;
import org.rdlinux.transactionalmq.core.service.MessagePublishService;
import org.rdlinux.transactionalmq.rabbitmq.RabbitMqProducerAdapter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * starter 自动装配测试
 */
public class TransactionalMqAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TransactionalMqAutoConfiguration.class));

    @Test
    public void should_register_message_publish_service_when_enabled() {
        this.contextRunner
                .withPropertyValues("transactionalmq.enabled=true",
                        "transactionalmq.dispatch-batch-size=23",
                        "transactionalmq.consume-record-retention-days=8")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .run(context -> {
                    Assert.assertTrue(context.containsBean("messagePublishService"));
                    Assert.assertNotNull(context.getBean(MessagePublishService.class));
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
    public void should_register_default_message_payload_serializer_and_round_trip_payload() {
        this.contextRunner.run(context -> {
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
                .withPropertyValues("transactionalmq.enabled=false")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .run(context -> {
                    Assert.assertFalse(context.containsBean("messagePublishService"));
                    Assert.assertFalse(context.containsBean("rabbitMqProducerAdapter"));
                    Assert.assertFalse(context.containsBean("transactionalMessageRepository"));
                });
    }

    @Test
    public void should_register_rabbit_mq_producer_adapter_when_rabbit_template_present() {
        this.contextRunner
                .withPropertyValues("transactionalmq.enabled=true")
                .withBean(EzDao.class, () -> mock(EzDao.class))
                .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .run(context -> {
                    Assert.assertTrue(context.containsBean("rabbitMqProducerAdapter"));
                    Assert.assertTrue(context.getBean(RabbitMqProducerAdapter.class) instanceof RabbitMqProducerAdapter);
                    Assert.assertTrue(context.containsBean("transactionalMessageRepository"));
                });
    }

    @Test
    public void should_be_listed_in_spring_factories_as_auto_configuration() {
        String className = TransactionalMqAutoConfiguration.class.getName();
        Assert.assertTrue(SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
                getClass().getClassLoader()).contains(className));
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
}
