package org.thingsboard.server.service.transport.msg;

import org.thingsboard.server.actors.device.SessionInfo;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Map;
import java.util.UUID;

public final class TransportToDeviceActorMsgWrapper implements TbActorMsg {
    public enum Kind {
        SESSION,
        TELEMETRY,
        SUBSCRIBE_RPC,
        ACTIVITY
    }

    private final SessionInfo sessionInfo;
    private final UUID sessionId;
    private final Kind kind;
    private final SessionEvent sessionEvent;
    private final Map<String, Object> telemetry;
    private final long activityTime;

    private TransportToDeviceActorMsgWrapper(SessionInfo sessionInfo, UUID sessionId, Kind kind,
                                             SessionEvent sessionEvent, Map<String, Object> telemetry,
                                             long activityTime) {
        this.sessionInfo = sessionInfo;
        this.sessionId = sessionId;
        this.kind = kind;
        this.sessionEvent = sessionEvent;
        this.telemetry = telemetry;
        this.activityTime = activityTime;
    }

    public static TransportToDeviceActorMsgWrapper session(UUID sessionId, SessionInfo info, SessionEvent event) {
        return new TransportToDeviceActorMsgWrapper(info, sessionId, Kind.SESSION, event, Map.of(), System.currentTimeMillis());
    }

    public static TransportToDeviceActorMsgWrapper telemetry(UUID sessionId, SessionInfo info, Map<String, Object> values) {
        return new TransportToDeviceActorMsgWrapper(info, sessionId, Kind.TELEMETRY, null, Map.copyOf(values), System.currentTimeMillis());
    }

    public static TransportToDeviceActorMsgWrapper subscribeRpc(UUID sessionId, SessionInfo info) {
        return new TransportToDeviceActorMsgWrapper(info, sessionId, Kind.SUBSCRIBE_RPC, null, Map.of(), System.currentTimeMillis());
    }

    public static TransportToDeviceActorMsgWrapper activity(UUID sessionId, SessionInfo info, long timestamp) {
        return new TransportToDeviceActorMsgWrapper(info, sessionId, Kind.ACTIVITY, null, Map.of(), timestamp);
    }

    public SessionInfo sessionInfo() {
        return sessionInfo;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public Kind kind() {
        return kind;
    }

    public SessionEvent sessionEvent() {
        return sessionEvent;
    }

    public Map<String, Object> telemetry() {
        return telemetry;
    }

    public long activityTime() {
        return activityTime;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG;
    }
}
