package org.example.thingsboard.transportdemo;

import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

/** 模拟 Core Service、Rule Engine 的消费者，便于观察消息实际落到哪条队列。 */
public final class DemoCoreService {
    private final InMemoryTransportQueueFactory queues;
    private final TransportApiRequestTemplate apiTemplate;

    public DemoCoreService(InMemoryTransportQueueFactory queues, TransportApiRequestTemplate apiTemplate) {
        this.queues = queues;
        this.apiTemplate = apiTemplate;
    }

    public boolean processOneTransportApiRequest() {
        TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg> request = queues.pollTransportApiRequest();
        if (request == null) {
            return false;
        }
        TransportProtos.TransportApiRequestMsg value = request.getValue();
        TransportProtos.DeviceInfoProto device = TransportProtos.DeviceInfoProto.newBuilder()
                .setDeviceName(value.hasValidateBasicMqttCredRequestMsg()
                        ? value.getValidateBasicMqttCredRequestMsg().getClientId() : "token-device")
                .setDeviceType("demo-device")
                .setTenantIdMSB(101)
                .setDeviceIdMSB(202)
                .build();
        TransportProtos.TransportApiResponseMsg response = TransportProtos.TransportApiResponseMsg.newBuilder()
                .setValidateCredResponseMsg(TransportProtos.ValidateDeviceCredentialsResponseMsg.newBuilder()
                        .setDeviceInfo(device)
                        .setCredentialsBody("validated-by-demo-core")
                        .build())
                .build();
        apiTemplate.respond(request.getKey(), response);
        return true;
    }

    public TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> processOneRuleEngineMessage() {
        return queues.pollRuleEngine();
    }

    public TbProtoQueueMsg<TransportProtos.ToCoreMsg> processOneCoreMessage() {
        return queues.pollCore();
    }

    public static String describeCoreMessage(TbProtoQueueMsg<TransportProtos.ToCoreMsg> message) {
        TransportProtos.TransportToDeviceActorMsg actor = message.getValue().getToDeviceActorMsg();
        if (actor.hasSessionEvent()) {
            return "session event: " + actor.getSessionEvent().getEvent();
        }
        if (actor.hasSubscribeToRPC()) {
            return "subscribe rpc: unsubscribe=" + actor.getSubscribeToRPC().getUnsubscribe();
        }
        return "device actor message";
    }
}
