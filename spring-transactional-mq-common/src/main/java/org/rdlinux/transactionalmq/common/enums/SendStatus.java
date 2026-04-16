package org.rdlinux.transactionalmq.common.enums;

/**
 * 发送历史状态。
 */
public enum SendStatus {
    INIT("init"),
    SENDING("sending"),
    SUCCESS("success"),
    FAILED("failed");

    private final String code;

    SendStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SendStatus fromCode(String code) {
        for (SendStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SendStatus code: " + code);
    }
}
