package org.example.thingsboard.actordemo;

import org.thingsboard.server.actors.AbstractTbActor;
import org.thingsboard.server.actors.InitFailureStrategy;
import org.thingsboard.server.actors.ProcessFailureStrategy;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

public final class DemoActor extends AbstractTbActor {
    private final DeviceState deviceState;
    private final DemoState metrics;
    private final int failuresBeforeReady;
    private int initAttempts;

    public DemoActor(DeviceState deviceState, DemoState metrics, int failuresBeforeReady) {
        this.deviceState = deviceState;
        this.metrics = metrics;
        this.failuresBeforeReady = failuresBeforeReady;
    }

    @Override
    public void init(org.thingsboard.server.actors.TbActorCtx ctx) throws TbActorException {
        if (initAttempts++ < failuresBeforeReady) {
            throw new TbActorException("demo init failure", new IllegalStateException("retryable"));
        }
        super.init(ctx);
    }

    @Override
    public boolean process(TbActorMsg msg) {
        DemoMessage demoMessage = (DemoMessage) msg;
        if (demoMessage.type() == DemoMessage.Type.FAIL) {
            throw new IllegalStateException("demo process failure");
        }
        metrics.onProcessStart(demoMessage.type().name());
        try {
            switch (demoMessage.type()) {
                case CONNECT -> deviceState.connect(demoMessage.value());
                case TELEMETRY -> deviceState.updateTelemetry(
                        demoMessage.value(), demoMessage.numericValue());
                case RPC_REQUEST -> deviceState.recordRpc(
                        demoMessage.value(), demoMessage.params());
                case FAIL -> throw new IllegalStateException("demo process failure");
            }
            return true;
        } finally {
            metrics.onProcessEnd();
        }
    }

    @Override
    public InitFailureStrategy onInitFailure(int attempt, Throwable error) {
        return InitFailureStrategy.retryWithDelay(20);
    }

    @Override
    public ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable error) {
        metrics.onProcessFailure();
        return ProcessFailureStrategy.resume();
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {
        metrics.onDestroy(stopReason);
    }
}
