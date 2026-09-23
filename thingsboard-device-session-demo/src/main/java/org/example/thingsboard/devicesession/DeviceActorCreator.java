package org.example.thingsboard.devicesession;

import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCreator;
import org.thingsboard.server.actors.TbActorId;

public final class DeviceActorCreator implements TbActorCreator {
    private final String deviceId;
    private final DeviceSessionCacheService sessionCache;
    private final DeviceEventSink sink;
    private final long sessionInactivityTimeout;

    public DeviceActorCreator(String deviceId, DeviceSessionCacheService sessionCache,
                              DeviceEventSink sink, long sessionInactivityTimeout) {
        this.deviceId = deviceId;
        this.sessionCache = sessionCache;
        this.sink = sink;
        this.sessionInactivityTimeout = sessionInactivityTimeout;
    }

    @Override
    public TbActorId createActorId() {
        return DeviceActor.actorId(deviceId);
    }

    @Override
    public TbActor createActor() {
        return new DeviceActor(deviceId, sessionCache, sink, sessionInactivityTimeout);
    }
}
