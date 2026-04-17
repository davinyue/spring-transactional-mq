package org.rdlinux.transactionalmq.core.mq;

import org.junit.Assert;
import org.junit.Test;
import org.rdlinux.transactionalmq.common.enums.MqType;
import org.rdlinux.transactionalmq.core.model.DispatchMessage;

import java.util.Arrays;

/**
 * MQ 生产者路由测试。
 */
public class MqProducerRouterTest {

    @Test
    public void sendShouldRouteByMqType() {
        CapturingMqProducerAdapter rabbitAdapter = new CapturingMqProducerAdapter(MqType.RABBITMQ);
        CapturingMqProducerAdapter kafkaAdapter = new CapturingMqProducerAdapter(MqType.KAFKA);
        MqProducerRouter router = new MqProducerRouter(Arrays.asList(rabbitAdapter, kafkaAdapter));
        DispatchMessage message = new DispatchMessage();
        message.setMqType(MqType.KAFKA);

        router.send(message);

        Assert.assertEquals(0, rabbitAdapter.sendCount);
        Assert.assertEquals(1, kafkaAdapter.sendCount);
    }

    @Test
    public void constructorShouldRejectDuplicateMqTypeAdapters() {
        try {
            new MqProducerRouter(Arrays.asList(
                    new CapturingMqProducerAdapter(MqType.RABBITMQ),
                    new CapturingMqProducerAdapter(MqType.RABBITMQ)));
            Assert.fail("expected IllegalStateException");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("RABBITMQ"));
        }
    }

    @Test
    public void sendShouldRejectUnsupportedMqType() {
        MqProducerRouter router = new MqProducerRouter(
                Arrays.asList(new CapturingMqProducerAdapter(MqType.RABBITMQ)));
        DispatchMessage message = new DispatchMessage();
        message.setMqType(MqType.KAFKA);

        try {
            router.send(message);
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("KAFKA"));
        }
    }

    private static class CapturingMqProducerAdapter implements MqProducerAdapter {

        private final MqType mqType;
        private int sendCount;

        private CapturingMqProducerAdapter(MqType mqType) {
            this.mqType = mqType;
        }

        @Override
        public MqType supportMqType() {
            return this.mqType;
        }

        @Override
        public void send(DispatchMessage message) {
            this.sendCount++;
        }
    }
}
