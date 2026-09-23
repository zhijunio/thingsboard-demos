package org.example.thingsboard.mqttdemo;

import com.google.protobuf.Descriptors;
import org.example.thingsboard.mqttdemo.adaptors.JsonMqttAdaptor;
import org.example.thingsboard.mqttdemo.adaptors.MqttTransportAdaptor;
import org.example.thingsboard.mqttdemo.adaptors.ProtoMqttAdaptor;
import org.example.thingsboard.mqttdemo.proto.DevicePayloadProtos;

/**
 * 对应源码中的 DeviceSessionCtx。
 *
 * 源码在 Device Profile 更新时动态计算 topic filter、payload type 和 protobuf descriptor。
 * 示例通过构造方法直接注入这些结果，省略 Profile、Spring 和数据库查询。
 */
public final class DeviceSessionCtx extends MqttDeviceAwareSessionContext {

    private final String telemetryTopic;
    private final String attributesTopic;
    private final TransportPayloadType payloadType;
    private final Descriptors.Descriptor telemetryDynamicMessageDescriptor;
    private final Descriptors.Descriptor attributesDynamicMessageDescriptor;
    private final Descriptors.Descriptor rpcResponseDynamicMessageDescriptor;
    private final Descriptors.Descriptor rpcRequestDynamicMessageDescriptor;
    private final MqttTransportAdaptor jsonAdaptor;
    private final MqttTransportAdaptor protoAdaptor;
    private boolean connected;
    private boolean provisionOnly;

    public DeviceSessionCtx(String telemetryTopic, String attributesTopic, TransportPayloadType payloadType) {
        this(telemetryTopic, attributesTopic, payloadType,
                DevicePayloadProtos.Telemetry.getDescriptor(),
                DevicePayloadProtos.Attributes.getDescriptor(),
                DevicePayloadProtos.RpcResponse.getDescriptor(),
                DevicePayloadProtos.RpcRequest.getDescriptor());
    }

    public DeviceSessionCtx(String telemetryTopic,
                            String attributesTopic,
                            TransportPayloadType payloadType,
                            Descriptors.Descriptor telemetryDynamicMessageDescriptor,
                            Descriptors.Descriptor attributesDynamicMessageDescriptor,
                            Descriptors.Descriptor rpcResponseDynamicMessageDescriptor,
                            Descriptors.Descriptor rpcRequestDynamicMessageDescriptor) {
        this.telemetryTopic = telemetryTopic;
        this.attributesTopic = attributesTopic;
        this.payloadType = payloadType;
        this.telemetryDynamicMessageDescriptor = telemetryDynamicMessageDescriptor;
        this.attributesDynamicMessageDescriptor = attributesDynamicMessageDescriptor;
        this.rpcResponseDynamicMessageDescriptor = rpcResponseDynamicMessageDescriptor;
        this.rpcRequestDynamicMessageDescriptor = rpcRequestDynamicMessageDescriptor;
        this.jsonAdaptor = new JsonMqttAdaptor();
        this.protoAdaptor = new ProtoMqttAdaptor();
    }

    public boolean isDeviceTelemetryTopic(String topicName) {
        return telemetryTopic.equals(topicName);
    }

    public boolean isDeviceAttributesTopic(String topicName) {
        return attributesTopic.equals(topicName);
    }

    public TransportPayloadType getPayloadType() {
        return payloadType;
    }

    public MqttTransportAdaptor getPayloadAdaptor() {
        return payloadType == TransportPayloadType.JSON ? jsonAdaptor : protoAdaptor;
    }

    public Descriptors.Descriptor getTelemetryDynamicMessageDescriptor() {
        return telemetryDynamicMessageDescriptor;
    }

    public Descriptors.Descriptor getAttributesDynamicMessageDescriptor() {
        return attributesDynamicMessageDescriptor;
    }

    public Descriptors.Descriptor getRpcResponseDynamicMessageDescriptor() {
        return rpcResponseDynamicMessageDescriptor;
    }

    public Descriptors.Descriptor getRpcRequestDynamicMessageDescriptor() {
        return rpcRequestDynamicMessageDescriptor;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setProvisionOnly(boolean provisionOnly) {
        this.provisionOnly = provisionOnly;
    }

    public boolean isProvisionOnly() {
        return provisionOnly;
    }
}
