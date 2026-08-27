package org.rdlinux.transactionalmq.api.consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 消费消息处理结果
 */
@Getter
@Setter
@Accessors(chain = true)
public class QueueMsgHandleRet {
    /**
     * 事务提交或回滚后回调
     */
    private List<FinallyCall> finallyCalls;
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
     *
     * @param unlockCall 事务提交或回滚后的回调
     * @return 当前处理结果对象
     */
    public QueueMsgHandleRet addFinallyCall(final FinallyCall unlockCall) {
        if (this.finallyCalls == null) {
            this.finallyCalls = new ArrayList<>();
        }
        this.finallyCalls.add(unlockCall);
        return this;
    }

    /**
     * 执行事务提交或回滚后回调
     *
     * @param e 消费过程中抛出的异常，无异常时为空
     */
    public void executeFinallyCall(Exception e) {
        if (this.finallyCalls != null) {
            for (FinallyCall call : this.finallyCalls) {
                call.call(e);
            }
        }
    }

    /**
     * 添加事务提交前回调
     *
     * @param commitCall 事务提交前的回调
     * @return 当前处理结果对象
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
     *
     * @return 默认提交结果
     */
    public static QueueMsgHandleRet DEFAULT() {
        return new QueueMsgHandleRet().setRollBack(false).setRollBackAck(false);
    }
}
