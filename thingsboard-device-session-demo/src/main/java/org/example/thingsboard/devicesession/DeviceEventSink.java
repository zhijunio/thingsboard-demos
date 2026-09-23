package org.example.thingsboard.devicesession;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DeviceEventSink {
    private final List<String> events = new CopyOnWriteArrayList<>();

    public void deviceConnected(String deviceId) {
        events.add("CONNECTED:" + deviceId);
    }

    public void deviceDisconnected(String deviceId, UUID sessionId, String reason) {
        events.add("DISCONNECTED:" + deviceId + ":" + sessionId + ":" + reason);
    }

    public void telemetry(String deviceId, UUID sessionId) {
        events.add("TELEMETRY:" + deviceId + ":" + sessionId);
    }

    public void rpc(String deviceId, UUID sessionId, ServerSideRpcRequest request) {
        events.add("RPC:" + deviceId + ":" + sessionId + ":" + request.method());
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
