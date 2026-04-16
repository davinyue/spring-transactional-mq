package org.rdlinux.transactionalmq.api.consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class QueueMsgHandleRet {
    /**
     * 事务提交或回滚后回调
     */
    private List<Runnable> finallyCalls;
    /**
     * 事务提交前回调
     */
    private List<Runnable> commitBeforeCalls;
    /**
     * 是否回滚事务
     */
    private boolean rollBack;
    /**
     * 回滚事务后是否自动提交ack
     */
    private boolean rollBackAck;

    /**
     * 添加事务提交或回滚后回调
     */
    public QueueMsgHandleRet addFinallyCall(final Runnable unlockCall) {
        if (this.finallyCalls == null) {
            this.finallyCalls = new ArrayList<>();
        }
        this.finallyCalls.add(unlockCall);
        return this;
    }

    /**
     * 执行事务提交或回滚后回调
     */
    public void executeFinallyCall() {
        if (this.finallyCalls != null) {
            for (Runnable call : this.finallyCalls) {
                call.run();
            }
        }
    }

    /**
     * 添加事务提交前回调
     */
    public QueueMsgHandleRet addCommitBeforeCall(final Runnable commitCall) {
        if (this.commitBeforeCalls == null) {
            this.commitBeforeCalls = new ArrayList<>();
        }
        this.commitBeforeCalls.add(commitCall);
        return this;
    }

    /**
     * 执行提交前回调
     */
    public void executeCommitCall() {
        if (this.commitBeforeCalls != null) {
            for (Runnable call : this.commitBeforeCalls) {
                call.run();
            }
        }
    }

    /**
     * 创建默认提交结果
     */
    public static QueueMsgHandleRet DEFAULT() {
        return new QueueMsgHandleRet().setRollBack(false).setRollBackAck(false);
    }
}
