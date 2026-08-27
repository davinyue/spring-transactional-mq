package org.rdlinux.transactionalmq.store.ezmybatis.repository;

import org.rdlinux.transactionalmq.common.enums.MessageStatus;
import org.rdlinux.transactionalmq.common.id.ObjectIdGenerator;
import org.rdlinux.transactionalmq.core.model.TransactionalMessageRecord;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageEntity;
import org.rdlinux.transactionalmq.store.ezmybatis.entity.TransactionalMessageHistoryEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 事务消息记录与实体之间的转换工具
 *
 */
final class TransactionalMessageEntityMapper {

    /**
     * 禁止实例化转换工具类
     */
    private TransactionalMessageEntityMapper() {
    }

    static TransactionalMessageEntity toEntity(TransactionalMessageRecord record) {
        if (record == null) {
            return null;
        }
        Date now = new Date();
        TransactionalMessageEntity entity = new TransactionalMessageEntity();
        entity.setId(resolvePrimaryKey(record.getId()));
        entity.setCreateTime(defaultDate(record.getCreateTime(), now));
        entity.setUpdateTime(defaultDate(record.getUpdateTime(), entity.getCreateTime()));
        entity.setMessageKey(record.getMessageKey());
        entity.setProducerCode(record.getProducerCode());
        entity.setMqType(record.getMqType());
        entity.setDestination(record.getDestination());
        entity.setRoute(record.getRoute());
        entity.setShardingKey(record.getShardingKey());
        entity.setPayloadText(defaultString(record.getPayloadText()));
        entity.setHeadersJson(toHeadersJson(record.getHeaders()));
        entity.setBizKey(record.getBizKey());
        entity.setMessageStatus(defaultMessageStatus(record.getMessageStatus()));
        entity.setNextDispatchTime(record.getNextDispatchTime());
        entity.setOriginalMessageId(record.getOriginalMessageId());
        entity.setRetryCount(defaultRetryCount(record.getRetryCount()));
        entity.setConsumerCode(record.getConsumerCode());
        entity.setLastError(record.getLastError());
        entity.setParentId(record.getParentId());
        entity.setRootId(record.getRootId());
        entity.setDispatchOwner(record.getDispatchOwner());
        entity.setDispatchToken(record.getDispatchToken());
        entity.setDispatchExpireTime(record.getDispatchExpireTime());
        return entity;
    }

    static TransactionalMessageRecord toRecord(TransactionalMessageEntity entity) {
        if (entity == null) {
            return null;
        }
        TransactionalMessageRecord record = new TransactionalMessageRecord();
        record.setId(entity.getId());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        record.setMessageKey(entity.getMessageKey());
        record.setProducerCode(entity.getProducerCode());
        record.setMqType(entity.getMqType());
        record.setDestination(entity.getDestination());
        record.setRoute(entity.getRoute());
        record.setShardingKey(entity.getShardingKey());
        record.setPayloadText(entity.getPayloadText());
        record.setHeaders(fromHeadersJson(entity.getHeadersJson()));
        record.setBizKey(entity.getBizKey());
        record.setMessageStatus(entity.getMessageStatus());
        record.setNextDispatchTime(entity.getNextDispatchTime());
        record.setOriginalMessageId(entity.getOriginalMessageId());
        record.setRetryCount(defaultRetryCount(entity.getRetryCount()));
        record.setConsumerCode(entity.getConsumerCode());
        record.setLastError(entity.getLastError());
        record.setParentId(entity.getParentId());
        record.setRootId(entity.getRootId());
        record.setDispatchOwner(entity.getDispatchOwner());
        record.setDispatchToken(entity.getDispatchToken());
        record.setDispatchExpireTime(entity.getDispatchExpireTime());
        return record;
    }

    static TransactionalMessageHistoryEntity toHistoryEntity(TransactionalMessageRecord record) {
        if (record == null) {
            return null;
        }
        Date now = new Date();
        TransactionalMessageHistoryEntity entity = new TransactionalMessageHistoryEntity();
        entity.setId(resolvePrimaryKey(record.getId()));
        entity.setCreateTime(defaultDate(record.getCreateTime(), now));
        entity.setUpdateTime(defaultDate(record.getUpdateTime(), entity.getCreateTime()));
        entity.setMessageKey(record.getMessageKey());
        entity.setProducerCode(record.getProducerCode());
        entity.setMqType(record.getMqType());
        entity.setDestination(record.getDestination());
        entity.setRoute(record.getRoute());
        entity.setShardingKey(record.getShardingKey());
        entity.setPayloadText(defaultString(record.getPayloadText()));
        entity.setHeadersJson(toHeadersJson(record.getHeaders()));
        entity.setBizKey(record.getBizKey());
        entity.setMessageStatus(defaultMessageStatus(record.getMessageStatus()));
        entity.setNextDispatchTime(record.getNextDispatchTime());
        entity.setOriginalMessageId(record.getOriginalMessageId());
        entity.setRetryCount(defaultRetryCount(record.getRetryCount()));
        entity.setConsumerCode(record.getConsumerCode());
        entity.setLastError(record.getLastError());
        entity.setParentId(record.getParentId());
        entity.setRootId(record.getRootId());
        entity.setDispatchOwner(record.getDispatchOwner());
        entity.setDispatchToken(record.getDispatchToken());
        entity.setDispatchExpireTime(record.getDispatchExpireTime());
        return entity;
    }

    /**
     * 使用默认时间填充空值
     *
     * @param date 原始时间
     * @param defaultDate 默认时间
     * @return 原始时间或默认时间
     */
    private static Date defaultDate(Date date, Date defaultDate) {
        return date == null ? defaultDate : date;
    }

    /**
     * 使用空字符串填充文本空值
     *
     * @param text 原始文本
     * @return 原始文本或空字符串
     */
    private static String defaultString(String text) {
        return text == null ? "" : text;
    }

    /**
     * 使用初始状态填充状态空值
     *
     * @param messageStatus 原始消息状态
     * @return 原始消息状态或初始状态
     */
    private static MessageStatus defaultMessageStatus(MessageStatus messageStatus) {
        return messageStatus == null ? MessageStatus.INIT : messageStatus;
    }

    /**
     * 使用零值填充重试次数空值
     *
     * @param retryCount 原始重试次数
     * @return 原始重试次数或零
     */
    private static int defaultRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : retryCount;
    }

    /**
     * 解析持久化主键
     *
     * <p>仅当 {@code id} 为空时才生成主键值，不会触碰业务消息标识</p>
     *
     * @param id 持久化主键
     * @return 可用于入库的主键
     */
    static String resolvePrimaryKey(String id) {
        if (id != null && !id.isEmpty()) {
            return id;
        }
        return ObjectIdGenerator.generate();
    }

    static String toHeadersJson(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder(64);
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"')
                    .append(':')
                    .append('"').append(escape(entry.getValue())).append('"');
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    static Map<String, String> fromHeadersJson(String headersJson) {
        Map<String, String> headers = new HashMap<>();
        if (headersJson == null) {
            return headers;
        }
        String text = headersJson.trim();
        if (text.length() < 2 || text.charAt(0) != '{' || text.charAt(text.length() - 1) != '}') {
            return headers;
        }
        String body = text.substring(1, text.length() - 1).trim();
        if (body.isEmpty()) {
            return headers;
        }
        int index = 0;
        while (index < body.length()) {
            ParseResult key = readQuoted(body, index);
            index = skipBlank(body, key.nextIndex);
            if (index < body.length() && body.charAt(index) == ':') {
                index++;
            }
            index = skipBlank(body, index);
            ParseResult value = readQuoted(body, index);
            headers.put(key.value, value.value);
            index = skipBlank(body, value.nextIndex);
            if (index < body.length() && body.charAt(index) == ',') {
                index++;
            }
            index = skipBlank(body, index);
        }
        return headers;
    }

    /**
     * 跳过文本中的空白字符
     *
     * @param text 待处理文本
     * @param index 起始位置
     * @return 下一个非空白字符位置
     */
    private static int skipBlank(String text, int index) {
        int cursor = index;
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    /**
     * 读取指定位置的引号包裹文本
     *
     * @param text 待读取文本
     * @param index 起始位置
     * @return 解析结果
     */
    private static ParseResult readQuoted(String text, int index) {
        int cursor = skipBlank(text, index);
        if (cursor >= text.length() || text.charAt(cursor) != '"') {
            return new ParseResult("", cursor);
        }
        cursor++;
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        while (cursor < text.length()) {
            char ch = text.charAt(cursor++);
            if (escaped) {
                builder.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                break;
            } else {
                builder.append(ch);
            }
        }
        return new ParseResult(builder.toString(), cursor);
    }

    /**
     * 转义 JSON 文本中的特殊字符
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 引号文本解析结果
     */
    private static final class ParseResult {
        /**
         * 解析出的文本值
         */
        private final String value;
        /**
         * 解析结束位置
         */
        private final int nextIndex;

        /**
         * 创建解析结果
         *
         * @param value 解析出的文本值
         * @param nextIndex 解析结束位置
         */
        private ParseResult(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

}
