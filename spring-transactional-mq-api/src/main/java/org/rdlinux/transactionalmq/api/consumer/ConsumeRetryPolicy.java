package org.rdlinux.transactionalmq.api.consumer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 消费失败重试策略
 *
 * <p>策略根据已经执行的重试次数，统一决定是否继续重试以及下次等待时间</p>
 */
public final class ConsumeRetryPolicy {

    /**
     * 策略类型
     */
    private enum PolicyType {
        NO_RETRY,
        FIXED_DELAY,
        CUSTOM_DELAYS,
        FIXED_DELAY_FOREVER
    }

    private static final ConsumeRetryPolicy NO_RETRY = new ConsumeRetryPolicy(
            PolicyType.NO_RETRY, 0, null, Collections.emptyList());

    /**
     * 策略类型
     */
    private final PolicyType policyType;
    /**
     * 最大重试次数
     */
    private final int maxRetryCount;
    /**
     * 固定重试间隔
     */
    private final Duration fixedDelay;
    /**
     * 自定义重试间隔
     */
    private final List<Duration> customDelays;

    /**
     * 创建消费重试策略
     *
     * @param policyType    策略类型
     * @param maxRetryCount 最大重试次数
     * @param fixedDelay    固定重试间隔
     * @param customDelays  自定义重试间隔
     */
    private ConsumeRetryPolicy(PolicyType policyType, int maxRetryCount, Duration fixedDelay,
                               List<Duration> customDelays) {
        this.policyType = policyType;
        this.maxRetryCount = maxRetryCount;
        this.fixedDelay = fixedDelay;
        this.customDelays = customDelays;
    }

    /**
     * 创建有限次数固定间隔策略
     *
     * @param maxRetryCount 最大重试次数
     * @param delay         每次重试间隔
     * @return 消费重试策略
     */
    public static ConsumeRetryPolicy fixedDelay(int maxRetryCount, Duration delay) {
        if (maxRetryCount < 1) {
            throw new IllegalArgumentException("maxRetryCount must be greater than zero");
        }
        validateDelay(delay);
        return new ConsumeRetryPolicy(PolicyType.FIXED_DELAY, maxRetryCount, delay,
                Collections.emptyList());
    }

    /**
     * 创建有限次数自定义间隔策略
     *
     * <p>间隔数量即最大重试次数，间隔不要求递增</p>
     *
     * @param delays 每次重试间隔
     * @return 消费重试策略
     */
    public static ConsumeRetryPolicy customDelays(Duration... delays) {
        if (delays == null || delays.length == 0) {
            throw new IllegalArgumentException("delays must not be empty");
        }
        Duration[] copiedDelays = delays.clone();
        for (Duration delay : copiedDelays) {
            validateDelay(delay);
        }
        return new ConsumeRetryPolicy(PolicyType.CUSTOM_DELAYS, copiedDelays.length, null,
                Collections.unmodifiableList(Arrays.asList(copiedDelays)));
    }

    /**
     * 创建无限次数固定间隔策略
     *
     * @param delay 每次重试间隔
     * @return 消费重试策略
     */
    public static ConsumeRetryPolicy fixedDelayForever(Duration delay) {
        validateDelay(delay);
        return new ConsumeRetryPolicy(PolicyType.FIXED_DELAY_FOREVER, Integer.MAX_VALUE, delay,
                Collections.emptyList());
    }

    /**
     * 获取不重试策略
     *
     * @return 不重试策略
     */
    public static ConsumeRetryPolicy noRetry() {
        return NO_RETRY;
    }

    /**
     * 获取下一次重试间隔
     *
     * <p>{@code retryCount=0} 表示原始消息首次消费失败，查询第一次重试间隔；
     * 返回空表示当前策略决定停止重试</p>
     *
     * @param retryCount 已经执行的重试次数
     * @return 下一次重试间隔；空表示停止重试
     */
    public Optional<Duration> nextDelay(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        switch (this.policyType) {
            case FIXED_DELAY:
                return retryCount < this.maxRetryCount ? Optional.of(this.fixedDelay) : Optional.empty();
            case CUSTOM_DELAYS:
                return retryCount < this.customDelays.size()
                        ? Optional.of(this.customDelays.get(retryCount)) : Optional.empty();
            case FIXED_DELAY_FOREVER:
                return Optional.of(this.fixedDelay);
            case NO_RETRY:
            default:
                return Optional.empty();
        }
    }

    /**
     * 校验重试间隔
     *
     * @param delay 重试间隔
     */
    private static void validateDelay(Duration delay) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            throw new IllegalArgumentException("delay must be greater than zero");
        }
    }
}
