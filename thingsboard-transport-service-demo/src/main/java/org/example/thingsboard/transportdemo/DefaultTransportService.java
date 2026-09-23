package org.example.thingsboard.transportdemo;

import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * ThingsBoard DefaultTransportService 的可运行缩减版。
 *
 * 保留源码中的关键分流：
 * - Core API 请求 -> Transport API request/response template
 * - telemetry/attributes -> ToRuleEngineMsg
 * - session/subscription -> ToCoreMsg.toDeviceActorMsg
 */
public final class DefaultTransportService implements TransportService {
    private final InMemoryTransportQueueFactory queues;
    private final TransportApiRequestTemplate transportApiRequestTemplate;

    public DefaultTransportService(InMemoryTransportQueueFactory queues) {
        this.queues = queues;
        this.transportApiRequestTemplate = new TransportApiRequestTemplate(queues);
    }

    @Override
    public CompletableFuture<TransportProtos.ValidateDeviceCredentialsResponseMsg> validateBasicMqttCredentials(
            TransportProtos.ValidateBasicMqttCredRequestMsg request) {
        TransportProtos.TransportApiRequestMsg apiRequest = TransportProtos.TransportApiRequestMsg.newBuilder()
                .setValidateBasicMqttCredRequestMsg(request)
                .build();
        TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg> queueMessage =
                new TbProtoQueueMsg<>(UUID.randomUUID(), apiRequest);
        return transportApiRequestTemplate.send(queueMessage)
                .thenApply(response -> response.getValue().getValidateCredResponseMsg());
    }

    @Override
    public CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                           TransportProtos.PostTelemetryMsg message) {
        for (TransportProtos.TsKvListProto values : message.getTsKvListList()) {
            MsgProtos.TbMsgProto tbMsg = newTbMsg(sessionInfo, "POST_TELEMETRY_REQUEST",
                    JsonPayload.toJson(values.getKvList()), values.getTs(), "telemetry");
            sendToRuleEngine(sessionInfo, tbMsg);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                           TransportProtos.PostAttributeMsg message) {
        String scope = message.getShared() ? "SHARED_SCOPE" : "CLIENT_SCOPE";
        MsgProtos.TbMsgProto tbMsg = newTbMsg(sessionInfo, "POST_ATTRIBUTES_REQUEST",
                JsonPayload.toJson(message.getKvList()), 0, scope, message.getShared());
        sendToRuleEngine(sessionInfo, tbMsg);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                           TransportProtos.SessionEventMsg message) {
        TransportProtos.TransportToDeviceActorMsg actorMessage = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo)
                .setSessionEvent(message)
                .build();
        sendToDeviceActor(sessionInfo, actorMessage);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> process(TransportProtos.SessionInfoProto sessionInfo,
                                           TransportProtos.SubscribeToRPCMsg message) {
        TransportProtos.TransportToDeviceActorMsg actorMessage = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo)
                .setSubscribeToRPC(message)
                .build();
        sendToDeviceActor(sessionInfo, actorMessage);
        return CompletableFuture.completedFuture(null);
    }

    private void sendToRuleEngine(TransportProtos.SessionInfoProto sessionInfo, MsgProtos.TbMsgProto tbMsg) {
        TransportProtos.ToRuleEngineMsg message = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                .setTbMsgProto(tbMsg)
                .build();
        queues.sendRuleEngine(new TbProtoQueueMsg<>(deviceId(sessionInfo), message));
    }

    private void sendToDeviceActor(TransportProtos.SessionInfoProto sessionInfo,
                                   TransportProtos.TransportToDeviceActorMsg actorMessage) {
        TransportProtos.ToCoreMsg message = TransportProtos.ToCoreMsg.newBuilder()
                .setToDeviceActorMsg(actorMessage)
                .build();
        queues.sendCore(new TbProtoQueueMsg<>(deviceId(sessionInfo), message));
    }

    private MsgProtos.TbMsgProto newTbMsg(TransportProtos.SessionInfoProto sessionInfo,
                                          String type, String data, long ts, String metadataValue) {
        return newTbMsg(sessionInfo, type, data, ts, metadataValue, false);
    }

    private MsgProtos.TbMsgProto newTbMsg(TransportProtos.SessionInfoProto sessionInfo,
                                          String type, String data, long ts, String metadataValue,
                                          boolean sharedAttributes) {
        MsgProtos.TbMsgMetaDataProto.Builder metadata = MsgProtos.TbMsgMetaDataProto.newBuilder()
                .putData("deviceName", sessionInfo.getDeviceName())
                .putData("deviceType", sessionInfo.getDeviceType())
                .putData("notifyDevice", "false");
        if (sharedAttributes) {
            metadata.putData("scope", "SHARED_SCOPE");
        }
        return MsgProtos.TbMsgProto.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType(type)
                .setEntityType("DEVICE")
                .setEntityIdMSB(sessionInfo.getDeviceIdMSB())
                .setEntityIdLSB(sessionInfo.getDeviceIdLSB())
                .setCustomerIdMSB(sessionInfo.getCustomerIdMSB())
                .setCustomerIdLSB(sessionInfo.getCustomerIdLSB())
                .setMetaData(metadata.build())
                .setData(data)
                .setTs(ts)
                .build();
    }

    private UUID deviceId(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB());
    }

    public TransportApiRequestTemplate transportApiRequestTemplate() {
        return transportApiRequestTemplate;
    }
}
