package org.rdlinux.transactionalmq.core.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;

/**
 * 识别数据库唯一键冲突。
 */
final class DuplicateKeyDetector {
    /**
     * 唯一键冲突的 SQLState。
     */
    private static final String UNIQUE_VIOLATION = "23505";

    /**
     * 禁止实例化。
     */
    private DuplicateKeyDetector() {
    }

    /**
     * 判断异常是否为唯一键冲突。
     *
     * @param exception 数据完整性异常
     * @return 是否为重复键
     */
    static boolean isDuplicate(DataIntegrityViolationException exception) {
        if (exception instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException) {
                for (SQLException sql = (SQLException) cause; sql != null; sql = sql.getNextException()) {
                    if (UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
