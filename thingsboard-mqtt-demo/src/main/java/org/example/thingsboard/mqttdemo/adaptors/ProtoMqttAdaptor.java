package org.example.thingsboard.mqttdemo.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.example.thingsboard.mqttdemo.DeviceSessionCtx;
import org.example.thingsboard.mqttdemo.MqttDeviceAwareSessionContext;
import org.example.thingsboard.mqttdemo.proto.TransportProtos;

/** 对应源码中的 ProtoMqttAdaptor。 */
public final class ProtoMqttAdaptor implements MqttTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx,
                                                                    MqttPublishMessage inbound)
            throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = deviceSession(ctx);
        return JsonMqttAdaptor.convertToPostTelemetry(dynamicJson(
                inbound, deviceSessionCtx.getTelemetryDynamicMessageDescriptor()));
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx,
                                                                      MqttPublishMessage inbound)
            throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = deviceSession(ctx);
        return JsonMqttAdaptor.convertToPostAttributes(dynamicJson(
                inbound, deviceSessionCtx.getAttributesDynamicMessageDescriptor()));
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx,
                                                                               MqttPublishMessage inbound,
                                                                               String topicBase)
            throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = deviceSession(ctx);
        JsonElement response = dynamicJson(inbound, deviceSessionCtx.getRpcResponseDynamicMessageDescriptor());
        return TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                .setRequestId(requestId(inbound, topicBase))
                .setPayload(response.toString())
                .build();
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx,
                                                                            MqttPublishMessage inbound,
                                                                            String topicBase)
            throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = deviceSession(ctx);
        JsonElement request = dynamicJson(inbound, deviceSessionCtx.getRpcRequestDynamicMessageDescriptor());
        return JsonMqttAdaptor.convertToServerRpcRequest(request, requestId(inbound, topicBase));
    }

    private static DeviceSessionCtx deviceSession(MqttDeviceAwareSessionContext ctx) throws AdaptorException {
        if (!(ctx instanceof DeviceSessionCtx deviceSessionCtx)) {
            throw new AdaptorException("ProtoMqttAdaptor requires DeviceSessionCtx descriptors");
        }
        return deviceSessionCtx;
    }

    private static JsonElement dynamicJson(MqttPublishMessage inbound,
                                           com.google.protobuf.Descriptors.Descriptor descriptor)
            throws AdaptorException {
        try {
            return ProtoConverter.dynamicMsgToJson(bytes(inbound), descriptor);
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            throw new AdaptorException("failed to decode dynamic protobuf payload", e);
        }
    }

    private static byte[] bytes(MqttPublishMessage inbound) {
        ByteBuf payload = inbound.payload();
        byte[] bytes = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), bytes);
        return bytes;
    }

    private static int requestId(MqttPublishMessage inbound, String topicBase) throws AdaptorException {
        String topic = inbound.variableHeader().topicName();
        if (!topic.startsWith(topicBase)) {
            throw new AdaptorException("topic does not start with " + topicBase);
        }
        try {
            return Integer.parseInt(topic.substring(topicBase.length()));
        } catch (NumberFormatException e) {
            throw new AdaptorException("invalid RPC request id", e);
        }
    }
}
