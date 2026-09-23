package org.example.thingsboard.devicesession;

import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbStringActorId;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.device.SessionInfo;
import org.thingsboard.server.actors.device.SessionInfoMetaData;
import org.thingsboard.server.actors.device.SessionTimeoutCheckMsg;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.service.transport.msg.SessionEvent;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 对应 ThingsBoard DeviceActor：一个设备的 session 状态在单 Actor 内串行修改。 */
public final class DeviceActor implements TbActor {
    private final String deviceId;
    private final DeviceSessionCacheService sessionCache;
    private final DeviceEventSink sink;
    private final long sessionInactivityTimeout;
    private final Map<UUID, SessionInfoMetaData> sessions = new LinkedHashMap<>();
    private TbActorCtx ctx;

    public DeviceActor(String deviceId, DeviceSessionCacheService sessionCache,
                       DeviceEventSink sink, long sessionInactivityTimeout) {
        this.deviceId = deviceId;
        this.sessionCache = sessionCache;
        this.sink = sink;
        this.sessionInactivityTimeout = sessionInactivityTimeout;
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        this.ctx = ctx;
        restoreSessions();
    }

    @Override
    public boolean process(TbActorMsg message) {
        if (message.getMsgType() == MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG) {
            processTransport((TransportToDeviceActorMsgWrapper) message);
        } else if (message.getMsgType() == MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG) {
            processRpc(((DeviceRpcRequestMsg) message).request());
        } else if (message.getMsgType() == MsgType.SESSION_TIMEOUT_MSG) {
            checkSessionsTimeout();
        }
        return true;
    }

    private void processTransport(TransportToDeviceActorMsgWrapper wrapper) {
        switch (wrapper.kind()) {
            case SESSION -> processSession(wrapper);
            case TELEMETRY -> {
                SessionInfoMetaData session = sessions.get(wrapper.sessionId());
                if (session != null) {
                    session.recordActivity(wrapper.activityTime());
                    sink.telemetry(deviceId, wrapper.sessionId());
                    dumpSessions();
                }
            }
            case SUBSCRIBE_RPC -> {
                SessionInfoMetaData session = sessions.get(wrapper.sessionId());
                if (session != null) {
                    session.subscribedToRpc(true);
                    session.recordActivity(wrapper.activityTime());
                    dumpSessions();
                }
            }
            case ACTIVITY -> {
                SessionInfoMetaData session = sessions.get(wrapper.sessionId());
                if (session != null) {
                    session.recordActivity(wrapper.activityTime());
                    dumpSessions();
                }
            }
        }
    }

    private void processSession(TransportToDeviceActorMsgWrapper wrapper) {
        if (wrapper.sessionEvent() == SessionEvent.OPEN) {
            if (sessions.containsKey(wrapper.sessionId())) {
                return;
            }
            sessions.put(wrapper.sessionId(), new SessionInfoMetaData(wrapper.sessionInfo(), wrapper.activityTime()));
            if (sessions.size() == 1) {
                sink.deviceConnected(deviceId);
            }
            dumpSessions();
        } else {
            SessionInfoMetaData removed = sessions.remove(wrapper.sessionId());
            if (removed != null) {
                if (sessions.isEmpty()) {
                    sink.deviceDisconnected(deviceId, wrapper.sessionId(), "CLOSED");
                }
                dumpSessions();
            }
        }
    }

    private void processRpc(ServerSideRpcRequest request) {
        sessions.forEach((sessionId, session) -> {
            if (session.subscribedToRpc()) {
                sink.rpc(deviceId, sessionId, request);
            }
        });
    }

    private void checkSessionsTimeout() {
        long expiration = System.currentTimeMillis() - sessionInactivityTimeout;
        new ArrayList<>(sessions.entrySet()).forEach(entry -> {
            if (entry.getValue().lastActivityTime() < expiration) {
                sessions.remove(entry.getKey());
                sink.deviceDisconnected(deviceId, entry.getKey(), "SESSION_TIMEOUT");
            }
        });
        if (!sessions.isEmpty()) {
            dumpSessions();
        } else {
            sessionCache.put(deviceId, List.of());
        }
    }

    private void restoreSessions() {
        sessionCache.get(deviceId).forEach(snapshot -> sessions.put(snapshot.sessionId(),
                new SessionInfoMetaData(snapshot.sessionInfo(), snapshot.lastActivityTime())));
    }

    private void dumpSessions() {
        sessionCache.put(deviceId, sessions.entrySet().stream()
                .map(entry -> new SessionSnapshot(entry.getKey(), entry.getValue().sessionInfo(), entry.getValue().lastActivityTime()))
                .toList());
    }

    public record SessionSnapshot(UUID sessionId, SessionInfo sessionInfo, long lastActivityTime) {
    }

    public static TbActorId actorId(String deviceId) {
        return new TbStringActorId("device:" + deviceId);
    }
}
