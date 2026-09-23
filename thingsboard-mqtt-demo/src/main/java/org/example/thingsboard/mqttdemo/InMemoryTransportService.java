package org.example.thingsboard.mqttdemo;

import com.google.protobuf.Message;
import org.example.thingsboard.mqttdemo.proto.TransportProtos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InMemoryTransportService implements TransportService {

    private final List<TransportEvent> events = new ArrayList<>();
    private final MqttAuthService authService;

    public InMemoryTransportService() {
        this(new InMemoryMqttAuthService("demo-token"));
    }

    public InMemoryTransportService(MqttAuthService authService) {
        this.authService = authService;
    }

    @Override
    public void process(DeviceTransportType transportType,
                        Message request,
                        TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        if (transportType != DeviceTransportType.MQTT) {
            callback.onError(new IllegalArgumentException("unsupported transport authentication request"));
            return;
        }
        boolean valid;
        String clientId = null;
        if (request instanceof TransportProtos.ValidateBasicMqttCredRequestMsg credentials) {
            clientId = credentials.getClientId();
            valid = authService.validateBasic(
                    clientId,
                    credentials.getUserName(),
                    credentials.getPassword().isEmpty() ? null : credentials.getPassword());
        } else if (request instanceof TransportProtos.ValidateDeviceX509CertRequestMsg credentials) {
            valid = authService.validateX509(credentials.getHash());
        } else {
            callback.onError(new IllegalArgumentException("unsupported MQTT credentials request"));
            return;
        }
        if (valid) {
            callback.onSuccess(new ValidateDeviceCredentialsResponse(clientId == null ? "x509-device" : clientId));
        } else {
            callback.onSuccess(new ValidateDeviceCredentialsResponse(null));
        }
    }

    @Override
    public void process(DeviceSessionCtx session, Message message, Map<String, String> metadata) {
        events.add(new TransportEvent(session.getSessionId(), message, metadata));
    }

    public List<TransportEvent> events() {
        return List.copyOf(events);
    }

    public record TransportEvent(java.util.UUID sessionId, Message message, Map<String, String> metadata) {
    }
}
