package org.example.thingsboard.transportdemo;

import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

public final class TransportServiceDemo {
    private TransportServiceDemo() {
    }

    public static void main(String[] args) throws Exception {
        InMemoryTransportQueueFactory queues = new InMemoryTransportQueueFactory();
        DefaultTransportService transportService = new DefaultTransportService(queues);
        DemoCoreService core = new DemoCoreService(queues, transportService.transportApiRequestTemplate());
        TransportProtos.SessionInfoProto session = DemoSessionInfo.create();

        var authFuture = transportService.validateBasicMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg.newBuilder()
                .setClientId("demo-device")
                .setUserName("demo-user")
                .setPassword("demo-password")
                .build());
        core.processOneTransportApiRequest();
        System.out.println("[Transport API] credentials: " + authFuture.get().getCredentialsBody());

        transportService.process(session, TransportProtos.PostTelemetryMsg.newBuilder()
                .addTsKvList(TransportProtos.TsKvListProto.newBuilder()
                        .setTs(1700000000000L)
                        .addKv(doubleValue("temperature", 23.5))
                        .addKv(longValue("counter", 7))
                        .build())
                .build());
        var telemetry = core.processOneRuleEngineMessage();
        System.out.println("[Rule Engine] type=" + telemetry.getValue().getTbMsgProto().getType()
                + ", data=" + telemetry.getValue().getTbMsgProto().getData());

        transportService.process(session, TransportProtos.PostAttributeMsg.newBuilder()
                .addKv(stringValue("firmware", "1.0.0"))
                .setShared(true)
                .build());
        var attributes = core.processOneRuleEngineMessage();
        System.out.println("[Rule Engine] type=" + attributes.getValue().getTbMsgProto().getType()
                + ", scope=" + attributes.getValue().getTbMsgProto().getMetaData().getDataOrThrow("scope"));

        transportService.process(session, TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(TransportProtos.SessionEvent.OPEN)
                .build());
        transportService.process(session, TransportProtos.SubscribeToRPCMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .build());
        System.out.println("[Core] " + DemoCoreService.describeCoreMessage(core.processOneCoreMessage()));
        System.out.println("[Core] " + DemoCoreService.describeCoreMessage(core.processOneCoreMessage()));
        System.out.println("完成：认证、Rule Engine 分流、Device Actor 分流均已执行。");
    }

    private static TransportProtos.KeyValueProto doubleValue(String key, double value) {
        return TransportProtos.KeyValueProto.newBuilder().setKey(key)
                .setType(TransportProtos.KeyValueType.DOUBLE_V).setDoubleV(value).build();
    }

    private static TransportProtos.KeyValueProto longValue(String key, long value) {
        return TransportProtos.KeyValueProto.newBuilder().setKey(key)
                .setType(TransportProtos.KeyValueType.LONG_V).setLongV(value).build();
    }

    private static TransportProtos.KeyValueProto stringValue(String key, String value) {
        return TransportProtos.KeyValueProto.newBuilder().setKey(key)
                .setType(TransportProtos.KeyValueType.STRING_V).setStringV(value).build();
    }
}
