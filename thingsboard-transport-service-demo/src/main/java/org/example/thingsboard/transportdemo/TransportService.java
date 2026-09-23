package org.example.thingsboard.transportdemo;

import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.CompletableFuture;

/** 只保留本示例使用到的 TransportService 操作。 */
public interface TransportService {
    CompletableFuture<TransportProtos.ValidateDeviceCredentialsResponseMsg> validateBasicMqttCredentials(
            TransportProtos.ValidateBasicMqttCredRequestMsg request);

    CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                   TransportProtos.SessionEventMsg message);

    CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                   TransportProtos.PostTelemetryMsg message);

    CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                   TransportProtos.PostAttributeMsg message);

    CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                   TransportProtos.SubscribeToRPCMsg message);
}
