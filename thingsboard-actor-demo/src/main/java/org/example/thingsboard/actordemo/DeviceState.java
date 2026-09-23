package org.example.thingsboard.actordemo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * State owned by one DemoActor. The actor is the only writer.
 */
public final class DeviceState {
    private String sessionId;
    private final Map<String, Double> telemetry = new LinkedHashMap<>();
    private String lastRpcMethod;
    private String lastRpcParams;

    void connect(String sessionId) {
        this.sessionId = sessionId;
    }

    void updateTelemetry(String key, double value) {
        telemetry.put(key, value);
    }

    void recordRpc(String method, String params) {
        lastRpcMethod = method;
        lastRpcParams = params;
    }

    public String sessionId() {
        return sessionId;
    }

    public Map<String, Double> telemetry() {
        return Map.copyOf(telemetry);
    }

    public String lastRpcMethod() {
        return lastRpcMethod;
    }

    public String lastRpcParams() {
        return lastRpcParams;
    }
}
