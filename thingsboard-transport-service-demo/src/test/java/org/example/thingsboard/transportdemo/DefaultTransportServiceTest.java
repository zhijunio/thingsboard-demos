package org.example.thingsboard.transportdemo;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTransportServiceTest {
    @Test
    void authenticationUsesTransportApiRequestResponseQueue() throws Exception {
        InMemoryTransportQueueFactory queues = new InMemoryTransportQueueFactory();
        DefaultTransportService service = new DefaultTransportService(queues);
        DemoCoreService core = new DemoCoreService(queues, service.transportApiRequestTemplate());

        var future = service.validateBasicMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg.newBuilder()
                .setClientId("device-1").build());
        assertTrue(core.processOneTransportApiRequest());
        assertEquals("validated-by-demo-core", future.get().getCredentialsBody());
    }

    @Test
    void telemetryAndAttributesGoToRuleEngine() {
        InMemoryTransportQueueFactory queues = new InMemoryTransportQueueFactory();
        DefaultTransportService service = new DefaultTransportService(queues);
        TransportProtos.SessionInfoProto session = DemoSessionInfo.create();

        service.process(session, TransportProtos.PostTelemetryMsg.newBuilder()
                .addTsKvList(TransportProtos.TsKvListProto.newBuilder()
                        .setTs(123).addKv(TransportProtos.KeyValueProto.newBuilder()
                                .setKey("temperature").setType(TransportProtos.KeyValueType.DOUBLE_V)
                                .setDoubleV(20.5)).build()).build());
        service.process(session, TransportProtos.PostAttributeMsg.newBuilder()
                .addKv(TransportProtos.KeyValueProto.newBuilder().setKey("version")
                        .setType(TransportProtos.KeyValueType.STRING_V).setStringV("1")).build());

        assertEquals("POST_TELEMETRY_REQUEST", queues.pollRuleEngine().getValue().getTbMsgProto().getType());
        assertEquals("POST_ATTRIBUTES_REQUEST", queues.pollRuleEngine().getValue().getTbMsgProto().getType());
    }

    @Test
    void sessionAndSubscriptionGoToDeviceActorThroughCore() {
        InMemoryTransportQueueFactory queues = new InMemoryTransportQueueFactory();
        DefaultTransportService service = new DefaultTransportService(queues);
        TransportProtos.SessionInfoProto session = DemoSessionInfo.create();

        service.process(session, TransportProtos.SessionEventMsg.newBuilder()
                .setEvent(TransportProtos.SessionEvent.OPEN).build());
        service.process(session, TransportProtos.SubscribeToRPCMsg.newBuilder().build());

        assertTrue(queues.pollCore().getValue().getToDeviceActorMsg().hasSessionEvent());
        assertTrue(queues.pollCore().getValue().getToDeviceActorMsg().hasSubscribeToRPC());
    }
}
