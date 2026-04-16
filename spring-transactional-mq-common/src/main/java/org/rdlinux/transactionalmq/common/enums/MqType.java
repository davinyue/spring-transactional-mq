package org.rdlinux.transactionalmq.common.enums;

/**
 * MQ 类型。
 */
public enum MqType {
    RABBITMQ("rabbitmq");

    private final String code;

    MqType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MqType fromCode(String code) {
        for (MqType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MqType code: " + code);
    }
}
