package org.rdlinux.transactionalmq.common.enums;

/**
 * 事务消息主状态
 */
public enum MessageStatus {
    INIT("init"),
    SENDING("sending"),
    SUCCESS("success"),
    ARCHIVING("archiving"),
    RETRYING("retrying"),
    DEAD("dead");

    /**
     * 状态编码
     */
    private final String code;

    MessageStatus(String code) {
        this.code = code;
    }

    /**
     * 获取状态编码
     *
     * @return 状态编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 根据编码获取事务消息状态
     *
     * @param code 状态编码
     * @return 对应的事务消息状态
     */
    public static MessageStatus fromCode(String code) {
        for (MessageStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown MessageStatus code: " + code);
    }
}
