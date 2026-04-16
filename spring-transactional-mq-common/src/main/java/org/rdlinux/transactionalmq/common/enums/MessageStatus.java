package org.rdlinux.transactionalmq.common.enums;

/**
 * 事务消息主状态。
 */
public enum MessageStatus {
    INIT("init"),
    SENDING("sending"),
    SUCCESS("success"),
    ARCHIVING("archiving"),
    RETRYING("retrying"),
    DEAD("dead");

    private final String code;

    MessageStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MessageStatus fromCode(String code) {
        for (MessageStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown MessageStatus code: " + code);
    }
}
