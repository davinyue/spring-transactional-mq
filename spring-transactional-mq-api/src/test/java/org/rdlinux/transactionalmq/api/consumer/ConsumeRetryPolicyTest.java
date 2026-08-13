package org.rdlinux.transactionalmq.api.consumer;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

/**
 * 消费失败重试策略测试
 */
public class ConsumeRetryPolicyTest {

    /**
     * 验证有限次数固定间隔策略
     */
    @Test
    public void fixedDelayShouldStopAfterMaxRetryCount() {
        ConsumeRetryPolicy policy = ConsumeRetryPolicy.fixedDelay(2, Duration.ofMinutes(1));

        Assert.assertEquals(Duration.ofMinutes(1), policy.nextDelay(0).get());
        Assert.assertEquals(Duration.ofMinutes(1), policy.nextDelay(1).get());
        Assert.assertFalse(policy.nextDelay(2).isPresent());
    }

    /**
     * 验证自定义间隔数量决定最大重试次数
     */
    @Test
    public void customDelaysShouldUseRetryCountAsIndex() {
        ConsumeRetryPolicy policy = ConsumeRetryPolicy.customDelays(
                Duration.ofMinutes(2), Duration.ofMinutes(2), Duration.ofMinutes(8));

        Assert.assertEquals(Duration.ofMinutes(2), policy.nextDelay(0).get());
        Assert.assertEquals(Duration.ofMinutes(2), policy.nextDelay(1).get());
        Assert.assertEquals(Duration.ofMinutes(8), policy.nextDelay(2).get());
        Assert.assertFalse(policy.nextDelay(3).isPresent());
    }

    /**
     * 验证无限固定间隔策略不会主动停止
     */
    @Test
    public void fixedDelayForeverShouldAlwaysReturnDelay() {
        ConsumeRetryPolicy policy = ConsumeRetryPolicy.fixedDelayForever(Duration.ofMinutes(1));

        Assert.assertEquals(Duration.ofMinutes(1), policy.nextDelay(0).get());
        Assert.assertEquals(Duration.ofMinutes(1), policy.nextDelay(Integer.MAX_VALUE).get());
    }

    /**
     * 验证不重试策略始终停止
     */
    @Test
    public void noRetryShouldAlwaysStop() {
        Assert.assertFalse(ConsumeRetryPolicy.noRetry().nextDelay(0).isPresent());
    }

    /**
     * 验证非法策略参数被拒绝
     */
    @Test
    public void invalidArgumentsShouldBeRejected() {
        this.assertIllegalArgument(() -> ConsumeRetryPolicy.fixedDelay(0, Duration.ofMinutes(1)));
        this.assertIllegalArgument(() -> ConsumeRetryPolicy.fixedDelay(1, Duration.ZERO));
        this.assertIllegalArgument(() -> ConsumeRetryPolicy.customDelays());
        this.assertIllegalArgument(() -> ConsumeRetryPolicy.customDelays(Duration.ofSeconds(-1)));
        this.assertIllegalArgument(() -> ConsumeRetryPolicy.noRetry().nextDelay(-1));
    }

    /**
     * 断言执行逻辑抛出参数异常
     *
     * @param runnable 待执行逻辑
     */
    private void assertIllegalArgument(Runnable runnable) {
        try {
            runnable.run();
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Assert.assertNotNull(expected.getMessage());
        }
    }
}
