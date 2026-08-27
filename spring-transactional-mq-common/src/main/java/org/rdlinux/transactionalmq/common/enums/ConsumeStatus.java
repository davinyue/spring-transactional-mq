package org.rdlinux.transactionalmq.common.enums;

import lombok.Getter;

/**
 * 消费状态
 */
@Getter
public enum ConsumeStatus {
    /**
     * 初始状态
     */
    INIT("init"),
    /**
     * 消费中状态
     */
    CONSUMING("consuming"),
    /**
     * 消费成功状态
     */
    SUCCESS("success"),
    /**
     * 消费失败状态
     */
    FAILED("failed");

    /**
     * 状态编码
     */
    private final String code;

    ConsumeStatus(String code) {
        this.code = code;
    }

    /**
     * 根据编码获取消费状态
     *
     * @param code 状态编码
     * @return 对应的消费状态
     */
    public static ConsumeStatus fromCode(String code) {
        for (ConsumeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ConsumeStatus code: " + code);
    }
}
