package org.rdlinux.transactionalmq.core.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TxnMqTransactionalService {

    /**
     * 请求一个新事务
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void requiresNew(Runnable call) {
        if (call != null) {
            call.run();
        }
    }

    /**
     * 如果当前有事务则加入当前事务, 否则创建新事务
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void required(Runnable call) {
        if (call != null) {
            call.run();
        }
    }
}
