package org.example.thingsboard.actordemo;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class DemoMessage implements TbActorMsg {
    private final int value;
    private final boolean fail;
    private final AtomicReference<TbActorStopReason> stoppedReason;

    public DemoMessage(int value) {
        this(value, false, null);
    }

    public DemoMessage(int value, boolean fail, AtomicReference<TbActorStopReason> stoppedReason) {
        this.value = value;
        this.fail = fail;
        this.stoppedReason = stoppedReason;
    }

    public int value() {
        return value;
    }

    public boolean fail() {
        return fail;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.DEMO_ACTOR_MESSAGE;
    }

    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        if (stoppedReason != null) {
            stoppedReason.set(Objects.requireNonNull(reason));
        }
    }
}
