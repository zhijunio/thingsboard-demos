package org.example.thingsboard.devicesession;

import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbActorSystem;
import org.thingsboard.server.actors.device.SessionInfo;
import org.thingsboard.server.actors.device.SessionType;
import org.thingsboard.server.service.transport.msg.SessionEvent;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.Map;
import java.util.UUID;

/** 简化 transport service 的认证、session 注册和消息投递边界。 */
public final class DeviceSessionManager {
    private final TbActorSystem actorSystem;
    private final DeviceSessionCacheService sessionCache;
    private final DeviceEventSink sink;
    private final long sessionInactivityTimeout;
    private final Map<String, String> accessTokens;

    public DeviceSessionManager(TbActorSystem actorSystem, DeviceSessionCacheService sessionCache,
                                DeviceEventSink sink, long sessionInactivityTimeout,
                                Map<String, String> accessTokens) {
        this.actorSystem = actorSystem;
        this.sessionCache = sessionCache;
        this.sink = sink;
        this.sessionInactivityTimeout = sessionInactivityTimeout;
        this.accessTokens = Map.copyOf(accessTokens);
    }

    public SessionHandle connect(String deviceId, String token, String nodeId) {
        if (!token.equals(accessTokens.get(deviceId))) {
            throw new SecurityException("invalid device credentials");
        }
        TbActorRef actor = actorSystem.createRootActor(new DeviceActorCreator(
                deviceId, sessionCache, sink, sessionInactivityTimeout));
        UUID sessionId = UUID.randomUUID();
        SessionInfo info = new SessionInfo(SessionType.ASYNC, nodeId);
        actor.tell(TransportToDeviceActorMsgWrapper.session(sessionId, info, SessionEvent.OPEN));
        return new SessionHandle(deviceId, sessionId, info);
    }

    public void telemetry(SessionHandle session, Map<String, Object> values) {
        actor(session).tell(TransportToDeviceActorMsgWrapper.telemetry(session.sessionId(), session.info(), values));
    }

    public void subscribeRpc(SessionHandle session) {
        actor(session).tell(TransportToDeviceActorMsgWrapper.subscribeRpc(session.sessionId(), session.info()));
    }

    public void activity(SessionHandle session, long timestamp) {
        actor(session).tell(TransportToDeviceActorMsgWrapper.activity(session.sessionId(), session.info(), timestamp));
    }

    public void close(SessionHandle session) {
        actor(session).tell(TransportToDeviceActorMsgWrapper.session(session.sessionId(), session.info(), SessionEvent.CLOSED));
    }

    public void sendRpc(String deviceId, ServerSideRpcRequest request) {
        actorSystem.tell(DeviceActor.actorId(deviceId), new DeviceRpcRequestMsg(request));
    }

    public void checkTimeout(String deviceId) {
        actorSystem.tell(DeviceActor.actorId(deviceId), org.thingsboard.server.actors.device.SessionTimeoutCheckMsg.instance());
    }

    private TbActorRef actor(SessionHandle session) {
        return actorSystem.createRootActor(new DeviceActorCreator(
                session.deviceId(), sessionCache, sink, sessionInactivityTimeout));
    }

    public record SessionHandle(String deviceId, UUID sessionId, SessionInfo info) {
    }
}
