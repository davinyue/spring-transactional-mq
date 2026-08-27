package org.rdlinux.transactionalmq.common.enums;

/**
 * MQ 类型
 */
public enum MqType {
    RABBITMQ("rabbitmq"),
    KAFKA("kafka");

    /**
     * MQ 类型编码
     */
    private final String code;

    MqType(String code) {
        this.code = code;
    }

    /**
     * 获取 MQ 类型编码
     *
     * @return MQ 类型编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 根据编码获取 MQ 类型
     *
     * @param code MQ 类型编码
     * @return 对应的 MQ 类型
     */
    public static MqType fromCode(String code) {
        for (MqType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MqType code: " + code);
    }
}
