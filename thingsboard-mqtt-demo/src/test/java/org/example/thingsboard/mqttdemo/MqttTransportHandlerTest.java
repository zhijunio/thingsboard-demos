package org.example.thingsboard.mqttdemo;

import org.example.thingsboard.mqttdemo.proto.TransportProtos;
import org.example.thingsboard.mqttdemo.proto.DevicePayloadProtos;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttTransportHandlerTest {

    @Test
    void connectMustAuthenticateBeforePublish() throws Exception {
        InMemoryTransportService transport = new InMemoryTransportService();
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);

        handler.processConnect(connect("device", "wrong-token"));
        assertFalse(handler.getDeviceSessionCtx().isConnected());
        handler.processPublish(handler.createPublishMessage(
                MqttTopics.DEVICE_TELEMETRY_TOPIC, "{\"temperature\":25}"));

        assertTrue(transport.events().isEmpty());
    }

    @Test
    void basicUsernameAndPasswordCredentialsAreSupported() {
        InMemoryTransportService transport = new InMemoryTransportService(
                new InMemoryMqttAuthService("unused-token", "device-user", "device-password", "unused-cert"));
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);

        handler.processConnect(connect("device", "device-user", "device-password"));

        assertTrue(handler.getDeviceSessionCtx().isConnected());
    }

    @Test
    void x509CredentialsUseTheCertificateValidationRequest() {
        InMemoryTransportService transport = new InMemoryTransportService(
                new InMemoryMqttAuthService("unused-token", "unused-user", "unused-password", "cert-sha3"));
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);

        handler.processConnect(null, connect("device", null, null), "cert-sha3");

        assertTrue(handler.getDeviceSessionCtx().isConnected());
    }

    @Test
    void provisionCredentialsBypassDeviceCredentialValidation() {
        InMemoryTransportService transport = new InMemoryTransportService(
                new InMemoryMqttAuthService("unused-token", "unused-user", "unused-password", "unused-cert"));
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);

        handler.processConnect(connect("provision", "provision", null));

        assertTrue(handler.getDeviceSessionCtx().isConnected());
        assertTrue(handler.getDeviceSessionCtx().isProvisionOnly());
    }

    @Test
    void tlsContextUsesJvmTrustManagersWhenNoCustomStoresAreProvided() throws Exception {
        SSLContext context = MqttBrokerDemo.createSslContext(
                "", "", "", "PKCS12", "", "", "PKCS12");

        assertEquals("TLS", context.getProtocol());
    }

    @Test
    void jsonTelemetryAndAttributesUseTheSelectedAdaptor() throws Exception {
        InMemoryTransportService transport = new InMemoryTransportService();
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);
        handler.processConnect(connect("device", "demo-token"));
        assertTrue(handler.getDeviceSessionCtx().isConnected());

        handler.processPublish(handler.createPublishMessage(
                MqttTopics.DEVICE_TELEMETRY_TOPIC, "{\"temperature\":25}"));
        handler.processPublish(handler.createPublishMessage(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC, "{\"firmware\":\"1.0\"}"));

        assertEquals(2, transport.events().size());
        TransportProtos.PostTelemetryMsg telemetry = assertInstanceOf(
                TransportProtos.PostTelemetryMsg.class, transport.events().get(0).message());
        assertEquals("temperature", telemetry.getTsKvList(0).getKv(0).getKey());
        assertInstanceOf(TransportProtos.PostAttributeMsg.class, transport.events().get(1).message());
    }

    @Test
    void protobufTelemetryIsConvertedToTransportMessage() throws Exception {
        InMemoryTransportService transport = new InMemoryTransportService();
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.PROTOBUF);
        handler.processConnect(connect("device", "demo-token"));
        assertTrue(handler.getDeviceSessionCtx().isConnected());
        DevicePayloadProtos.Telemetry payload = DevicePayloadProtos.Telemetry.newBuilder()
                .setTemperature(25.5)
                .setActive(true)
                .build();

        handler.processPublish(handler.createPublishMessage(
                MqttTopics.DEVICE_TELEMETRY_TOPIC, payload.toByteArray()));

        TransportProtos.PostTelemetryMsg result = assertInstanceOf(
                TransportProtos.PostTelemetryMsg.class, transport.events().get(0).message());
        assertEquals(25.5, result.getTsKvList(0).getKv(0).getDoubleV());
    }

    @Test
    void rpcRequestIdComesFromTheTopic() throws Exception {
        InMemoryTransportService transport = new InMemoryTransportService();
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);
        handler.processConnect(connect("device", "demo-token"));
        assertTrue(handler.getDeviceSessionCtx().isConnected());

        handler.processPublish(handler.createPublishMessage(
                MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + "42",
                "{\"method\":\"reboot\",\"params\":{\"delay\":1}}"));

        TransportProtos.ToServerRpcRequestMsg result = assertInstanceOf(
                TransportProtos.ToServerRpcRequestMsg.class, transport.events().get(0).message());
        assertEquals(42, result.getRequestId());
        assertEquals("reboot", result.getMethodName());
        assertEquals("{\"delay\":1}", result.getParams());
    }

    @Test
    void unknownTopicDoesNotEnterTransportService() throws Exception {
        InMemoryTransportService transport = new InMemoryTransportService();
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);
        handler.processConnect(connect("device", "demo-token"));
        assertTrue(handler.getDeviceSessionCtx().isConnected());

        handler.processPublish(handler.createPublishMessage("v1/devices/me/unknown", "{}"));

        assertTrue(transport.events().isEmpty());
    }

    @Test
    void messagesReceivedBeforeConnectAreProcessedAfterAuthentication() throws Exception {
        InMemoryTransportService transport = new InMemoryTransportService();
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);

        handler.processMqttMsg(handler.createPublishMessage(
                MqttTopics.DEVICE_TELEMETRY_TOPIC, "{\"temperature\":25}"));
        assertTrue(transport.events().isEmpty());

        handler.processMqttMsg(connect("device", "demo-token"));

        assertTrue(handler.getDeviceSessionCtx().isConnected());
        assertEquals(1, transport.events().size());
        assertInstanceOf(MqttConnAckMessage.class, handler.outboundMessages().get(0));
    }

    @Test
    void controlMessagesFollowMqttSessionLifecycle() throws Exception {
        InMemoryTransportService transport = new InMemoryTransportService();
        MqttTransportHandler handler = new MqttTransportHandler(
                new MqttTransportContext(transport), TransportPayloadType.JSON);
        handler.processMqttMsg(connect("device", "demo-token"));

        handler.processMqttMsg(subscribe(7, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + "+"));
        handler.processMqttMsg(control(MqttMessageType.PINGREQ));
        handler.processMqttMsg(unsubscribe(8, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + "+"));

        assertInstanceOf(MqttConnAckMessage.class, handler.outboundMessages().get(0));
        assertInstanceOf(MqttSubAckMessage.class, handler.outboundMessages().get(1));
        assertEquals(MqttMessageType.PINGRESP, handler.outboundMessages().get(2).fixedHeader().messageType());
        assertInstanceOf(MqttUnsubAckMessage.class, handler.outboundMessages().get(3));

        handler.processMqttMsg(control(MqttMessageType.DISCONNECT));

        assertFalse(handler.getDeviceSessionCtx().isConnected());
    }

    private static MqttConnectMessage connect(String clientId, String accessToken) {
        return connect(clientId, accessToken, null);
    }

    private static MqttConnectMessage connect(String clientId, String userName, String password) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                "MQTT", 4, true, false, false, 0, false, true, 60);
        MqttConnectPayload payload = new MqttConnectPayload(
                clientId, null, (byte[]) null, userName, password == null ? null : password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new MqttConnectMessage(fixedHeader, variableHeader, payload);
    }

    private static MqttSubscribeMessage subscribe(int messageId, String topic) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        return new MqttSubscribeMessage(
                fixedHeader,
                MqttMessageIdVariableHeader.from(messageId),
                new MqttSubscribePayload(java.util.List.of(
                        new MqttTopicSubscription(topic, MqttQoS.AT_LEAST_ONCE))));
    }

    private static MqttUnsubscribeMessage unsubscribe(int messageId, String topic) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        return new MqttUnsubscribeMessage(
                fixedHeader,
                MqttMessageIdVariableHeader.from(messageId),
                new MqttUnsubscribePayload(java.util.List.of(topic)));
    }

    private static MqttMessage control(MqttMessageType type) {
        return new MqttMessage(new MqttFixedHeader(
                type, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }
}
