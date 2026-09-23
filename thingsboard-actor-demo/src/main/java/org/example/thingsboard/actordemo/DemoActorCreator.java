package org.example.thingsboard.actordemo;

import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCreator;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbStringActorId;

public final class DemoActorCreator implements TbActorCreator {
    private final TbActorId actorId;
    private final DeviceState deviceState;
    private final DemoState metrics;
    private final int failuresBeforeReady;

    public DemoActorCreator(String actorId, DeviceState deviceState, DemoState metrics,
                            int failuresBeforeReady) {
        this.actorId = new TbStringActorId(actorId);
        this.deviceState = deviceState;
        this.metrics = metrics;
        this.failuresBeforeReady = failuresBeforeReady;
    }

    @Override
    public TbActorId createActorId() {
        return actorId;
    }

    @Override
    public TbActor createActor() {
        return new DemoActor(deviceState, metrics, failuresBeforeReady);
    }
}
