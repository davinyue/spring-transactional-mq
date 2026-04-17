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
        entity.setParentId(record.getParentId());
        entity.setRootId(record.getRootId());
        entity.setDispatchOwner(record.getDispatchOwner());
        entity.setDispatchToken(record.getDispatchToken());
        entity.setDispatchExpireTime(record.getDispatchExpireTime());
        return entity;
    }

    private static Date defaultDate(Date date, Date defaultDate) {
        return date == null ? defaultDate : date;
    }

    private static String defaultString(String text) {
        return text == null ? "" : text;
    }

    private static MessageStatus defaultMessageStatus(MessageStatus messageStatus) {
        return messageStatus == null ? MessageStatus.INIT : messageStatus;
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
        Map<String, String> headers = new HashMap<String, String>();
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

    private static int skipBlank(String text, int index) {
        int cursor = index;
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

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

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class ParseResult {
        private final String value;
        private final int nextIndex;

        private ParseResult(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

}
