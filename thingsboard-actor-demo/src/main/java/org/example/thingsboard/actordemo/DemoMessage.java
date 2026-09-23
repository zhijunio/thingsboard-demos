package org.example.thingsboard.actordemo;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class DemoMessage implements TbActorMsg {
    public enum Type {
        CONNECT,
        TELEMETRY,
        RPC_REQUEST,
        FAIL
    }

    private final Type type;
    private final String value;
    private final double numericValue;
    private final String params;
    private final AtomicReference<TbActorStopReason> stoppedReason;

    private DemoMessage(Type type, String value, double numericValue, String params,
                        AtomicReference<TbActorStopReason> stoppedReason) {
        this.type = type;
        this.value = value;
        this.numericValue = numericValue;
        this.params = params;
        this.stoppedReason = stoppedReason;
    }

    public static DemoMessage connect(String sessionId) {
        return new DemoMessage(Type.CONNECT, sessionId, 0, null, null);
    }

    public static DemoMessage telemetry(String key, double value) {
        return new DemoMessage(Type.TELEMETRY, key, value, null, null);
    }

    public static DemoMessage rpcRequest(String method, String params) {
        return new DemoMessage(Type.RPC_REQUEST, method, 0, params, null);
    }

    public static DemoMessage failure() {
        return new DemoMessage(Type.FAIL, null, 0, null, null);
    }

    public static DemoMessage telemetry(String key, double value,
                                        AtomicReference<TbActorStopReason> stoppedReason) {
        return new DemoMessage(Type.TELEMETRY, key, value, null, stoppedReason);
    }

    public Type type() {
        return type;
    }

    public String value() {
        return value;
    }

    public double numericValue() {
        return numericValue;
    }

    public String params() {
        return params;
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
