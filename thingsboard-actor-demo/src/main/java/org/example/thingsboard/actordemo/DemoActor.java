package org.example.thingsboard.actordemo;

import org.thingsboard.server.actors.AbstractTbActor;
import org.thingsboard.server.actors.InitFailureStrategy;
import org.thingsboard.server.actors.ProcessFailureStrategy;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

public final class DemoActor extends AbstractTbActor {
    private final DemoState state;
    private final int failuresBeforeReady;
    private int initAttempts;

    public DemoActor(DemoState state, int failuresBeforeReady) {
        this.state = state;
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
        if (demoMessage.fail()) {
            throw new IllegalStateException("demo process failure");
        }
        state.onProcessStart(demoMessage.value());
        try {
            return true;
        } finally {
            state.onProcessEnd();
        }
    }

    @Override
    public InitFailureStrategy onInitFailure(int attempt, Throwable error) {
        return InitFailureStrategy.retryWithDelay(20);
    }

    @Override
    public ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable error) {
        state.onProcessFailure();
        return ProcessFailureStrategy.resume();
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {
        state.onDestroy(stopReason);
    }
}
