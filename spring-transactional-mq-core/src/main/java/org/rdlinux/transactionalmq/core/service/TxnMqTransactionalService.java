package org.rdlinux.transactionalmq.core.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务消息事务边界辅助服务
 */
public class TxnMqTransactionalService {

    /**
     * 请求一个新事务
     *
     * @param call 事务内执行的回调
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void requiresNew(Runnable call) {
        if (call != null) {
            call.run();
        }
    }

    /**
     * 如果当前有事务则加入当前事务, 否则创建新事务
     *
     * @param call 事务内执行的回调
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void required(Runnable call) {
        if (call != null) {
            call.run();
        }
    }
}
