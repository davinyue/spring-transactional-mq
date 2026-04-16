package org.rdlinux.transactionalmq.rabbitmq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * RabbitMQ 消息体 gzip 编解码工具。
 */
public final class RabbitMqPayloadCodec {

    private RabbitMqPayloadCodec() {
    }

    public static byte[] gzip(String payloadText) {
        if (payloadText == null) {
            return new byte[0];
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(payloadText.getBytes(StandardCharsets.UTF_8));
            gzipOutputStream.finish();
            gzipOutputStream.close();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to gzip rabbitmq payload", ex);
        }
    }

    public static String decode(byte[] body, String contentEncoding) {
        if (body == null || body.length == 0) {
            return "";
        }
        if (!"gzip".equalsIgnoreCase(contentEncoding)) {
            return new String(body, StandardCharsets.UTF_8);
        }
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(body));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int length = gzipInputStream.read(buffer);
            while (length >= 0) {
                if (length > 0) {
                    outputStream.write(buffer, 0, length);
                }
                length = gzipInputStream.read(buffer);
            }
            gzipInputStream.close();
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to gunzip rabbitmq payload", ex);
        }
    }
}
