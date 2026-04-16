package org.rdlinux.transactionalmq.api.model;

/**
 * 发送结果。
 */
public class SendResult {

    /**
     * 接收结果码。
     */
    public static final String RESULT_CODE_ACCEPTED = "accepted";
    /**
     * 拒绝结果码。
     */
    public static final String RESULT_CODE_REJECTED = "rejected";

    /**
     * 结果码。
     */
    private final String resultCode;
    /**
     * 是否被接收。
     */
    private final boolean accepted;
    /**
     * 消息主键。
     */
    private final String id;
    /**
     * 消息键。
     */
    private final String messageKey;
    /**
     * 结果描述。
     */
    private final String description;

    /**
     * 构造发送结果。
     *
     * @param resultCode 结果码
     * @param accepted 是否被接收
     * @param id 消息主键
     * @param messageKey 消息键
     * @param description 结果描述
     */
    public SendResult(String resultCode, boolean accepted, String id, String messageKey, String description) {
        this.resultCode = resultCode;
        this.accepted = accepted;
        this.id = id;
        this.messageKey = messageKey;
        this.description = description;
    }

    /**
     * 创建已接收结果。
     *
     * @param id 消息主键
     * @param messageKey 消息键
     * @return 发送结果
     */
    public static SendResult accepted(String id, String messageKey) {
        return new SendResult(RESULT_CODE_ACCEPTED, true, id, messageKey, null);
    }

    /**
     * 创建拒绝结果。
     *
     * @param messageKey 消息键
     * @param description 结果描述
     * @return 发送结果
     */
    public static SendResult rejected(String messageKey, String description) {
        return new SendResult(RESULT_CODE_REJECTED, false, null, messageKey, description);
    }

    /**
     * 获取结果码。
     *
     * @return 结果码
     */
    public String getResultCode() {
        return resultCode;
    }

    /**
     * 是否被接收。
     *
     * @return 是否被接收
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * 获取消息主键。
     *
     * @return 消息主键
     */
    public String getId() {
        return id;
    }

    /**
     * 获取消息键。
     *
     * @return 消息键
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * 获取结果描述。
     *
     * @return 结果描述
     */
    public String getDescription() {
        return description;
    }
}
