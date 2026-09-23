package org.example.thingsboard.mqttdemo.adaptors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import org.example.thingsboard.mqttdemo.DeviceSessionCtx;
import org.example.thingsboard.mqttdemo.MqttDeviceAwareSessionContext;
import org.example.thingsboard.mqttdemo.proto.TransportProtos;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;

/** 对应源码中的 MqttTransportAdaptor，保留本示例涉及的转换方法。 */
public interface MqttTransportAdaptor {

    TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx,
                                                            MqttPublishMessage inbound) throws AdaptorException;

    TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx,
                                                              MqttPublishMessage inbound) throws AdaptorException;

    TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx,
                                                                       MqttPublishMessage inbound,
                                                                       String topicBase) throws AdaptorException;

    TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx,
                                                                    MqttPublishMessage inbound,
                                                                    String topicBase) throws AdaptorException;

    default MqttPublishMessage createMqttPublishMsg(DeviceSessionCtx ctx, String topic, byte[] payload) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH, false, ctx.getQoSForTopic(topic), false, 0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, ctx.nextMsgId());
        ByteBuf buffer = Unpooled.wrappedBuffer(payload);
        return new MqttPublishMessage(fixedHeader, variableHeader, buffer);
    }

    default MqttPublishMessage createMqttPublishMsg(DeviceSessionCtx ctx, String topic, String payload) {
        return createMqttPublishMsg(ctx, topic, payload.getBytes(StandardCharsets.UTF_8));
    }
}
