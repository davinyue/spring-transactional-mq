package org.rdlinux.transactionalmq.common.id;

import org.rdlinux.id.objectid.ObjectId;

/**
 * ObjectId 生成器。
 */
public final class ObjectIdGenerator {

    private ObjectIdGenerator() {
    }

    /**
     * 生成 24 位十六进制 ObjectId。
     *
     * @return 24 位十六进制 ObjectId
     */
    public static String generate() {
        return new ObjectId().toHexString();
    }
}
