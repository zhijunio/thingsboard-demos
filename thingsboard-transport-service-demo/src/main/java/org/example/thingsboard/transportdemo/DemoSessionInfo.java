package org.example.thingsboard.transportdemo;

import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

final class DemoSessionInfo {
    private DemoSessionInfo() {
    }

    static TransportProtos.SessionInfoProto create() {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID deviceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        return TransportProtos.SessionInfoProto.newBuilder()
                .setNodeId("transport-demo")
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(101)
                .setTenantIdLSB(102)
                .setDeviceIdMSB(deviceId.getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getLeastSignificantBits())
                .setDeviceName("demo-device")
                .setDeviceType("demo-device")
                .setDeviceProfileIdMSB(201)
                .setDeviceProfileIdLSB(202)
                .setCustomerIdMSB(301)
                .setCustomerIdLSB(302)
                .build();
    }
}
