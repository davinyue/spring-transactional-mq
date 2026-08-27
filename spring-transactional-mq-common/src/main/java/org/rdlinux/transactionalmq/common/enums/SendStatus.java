package org.rdlinux.transactionalmq.common.enums;

/**
 * 发送历史状态
 */
public enum SendStatus {
    INIT("init"),
    SENDING("sending"),
    SUCCESS("success"),
    FAILED("failed");

    /**
     * 状态编码
     */
    private final String code;

    SendStatus(String code) {
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
     * 根据编码获取发送状态
     *
     * @param code 状态编码
     * @return 对应的发送状态
     */
    public static SendStatus fromCode(String code) {
        for (SendStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SendStatus code: " + code);
    }
}
