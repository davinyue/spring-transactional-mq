package org.rdlinux.transactionalmq.api.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 消费上下文。
 */
@Setter
@Accessors(chain = true)
public class ConsumeContext {

    /**
     * 消息主键
     */
    @Getter
    private String id;
    /**
     * 消息键
     */
    @Getter
    private String messageKey;
    /**
     * 父消息 id
     */
    @Getter
    private String parentId;
    /**
     * 根消息 id
     */
    @Getter
    private String rootId;
    /**
     * 原始消息头
     */
    private Map<String, String> headers = new HashMap<>();
    /**
     * 消费者编码。
     */
    @Getter
    private String consumerCode;

    /**
     * 设置原始消息头。
     *
     * @param headers 原始消息头
     * @return 当前上下文对象
     */
    public ConsumeContext setHeaders(Map<String, String> headers) {
        if (headers == null) {
            this.headers = new HashMap<>();
        } else {
            this.headers = new HashMap<>(headers);
        }
        return this;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(this.headers);
    }

}
