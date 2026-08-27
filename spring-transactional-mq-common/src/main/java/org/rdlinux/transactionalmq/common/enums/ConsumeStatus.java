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

    private final String code;

    ConsumeStatus(String code) {
        this.code = code;
    }

    public static ConsumeStatus fromCode(String code) {
        for (ConsumeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ConsumeStatus code: " + code);
    }
}
