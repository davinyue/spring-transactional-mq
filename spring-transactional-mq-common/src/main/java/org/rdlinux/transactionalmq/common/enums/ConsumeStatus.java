package org.rdlinux.transactionalmq.common.enums;

import lombok.Getter;

/**
 * 消费状态
 */
@Getter
public enum ConsumeStatus {
    INIT("init"),
    CONSUMING("consuming"),
    SUCCESS("success"),
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
