package org.example.thingsboard.mqttdemo;

import org.example.thingsboard.mqttdemo.proto.TransportProtos;
import org.example.thingsboard.mqttdemo.proto.DevicePayloadProtos;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.nio.charset.StandardCharsets;

/** 无 Broker 的源码路径演示入口。 */
public final class MqttSourceDemo {

    private MqttSourceDemo() {
    }

    public static void main(String[] args) throws Exception {
        InMemoryTransportService jsonTransport = new InMemoryTransportService();
        MqttTransportHandler jsonHandler = new MqttTransportHandler(
                new MqttTransportContext(jsonTransport), TransportPayloadType.JSON);
        jsonHandler.processConnect(connect("json-device", "demo-token"));
        System.out.println("JSON CONNECT: " + jsonHandler.getDeviceSessionCtx().isConnected());
        jsonHandler.processPublish(jsonHandler.createPublishMessage(
                MqttTopics.DEVICE_TELEMETRY_TOPIC, "{\"temperature\":25.3,\"active\":true}"));
        jsonHandler.processPublish(jsonHandler.createPublishMessage(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC, "{\"firmware\":\"1.0.0\"}"));
        jsonHandler.processPublish(jsonHandler.createPublishMessage(
                MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + "42", "{\"method\":\"reboot\",\"params\":{}}"));

        InMemoryTransportService basicTransport = new InMemoryTransportService(
                new InMemoryMqttAuthService("unused-token", "demo-user", "demo-password", "unused-cert"));
        MqttTransportHandler basicHandler = new MqttTransportHandler(
                new MqttTransportContext(basicTransport), TransportPayloadType.JSON);
        basicHandler.processConnect(connect("basic-device", "demo-user", "demo-password"));
        System.out.println("Basic username/password CONNECT: " + basicHandler.getDeviceSessionCtx().isConnected());

        InMemoryTransportService x509Transport = new InMemoryTransportService(
                new InMemoryMqttAuthService("unused-token", "unused-user", "unused-password", "demo-certificate-sha3"));
        MqttTransportHandler x509Handler = new MqttTransportHandler(
                new MqttTransportContext(x509Transport), TransportPayloadType.JSON);
        x509Handler.processConnect(null, connect("x509-device", null, null), "demo-certificate-sha3");
        System.out.println("X.509 CONNECT: " + x509Handler.getDeviceSessionCtx().isConnected());

        MqttTransportHandler provisionHandler = new MqttTransportHandler(
                new MqttTransportContext(new InMemoryTransportService()), TransportPayloadType.JSON);
        provisionHandler.processConnect(connect("provision", "provision", null));
        System.out.println("Provision CONNECT: " + provisionHandler.getDeviceSessionCtx().isConnected());

        InMemoryTransportService protoTransport = new InMemoryTransportService();
        MqttTransportHandler protoHandler = new MqttTransportHandler(
                new MqttTransportContext(protoTransport), TransportPayloadType.PROTOBUF);
        protoHandler.processConnect(connect("proto-device", "demo-token"));
        System.out.println("Protobuf CONNECT: " + protoHandler.getDeviceSessionCtx().isConnected());
        DevicePayloadProtos.Telemetry telemetry = DevicePayloadProtos.Telemetry.newBuilder()
                .setTemperature(26.1)
                .setActive(true)
                .build();
        protoHandler.processPublish(protoHandler.createPublishMessage(
                MqttTopics.DEVICE_TELEMETRY_TOPIC, telemetry.toByteArray()));

        printEvents("JSON", jsonTransport);
        printEvents("Protobuf", protoTransport);
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
                clientId, null, (byte[]) null, userName,
                password == null ? null : password.getBytes(StandardCharsets.UTF_8));
        return new MqttConnectMessage(fixedHeader, variableHeader, payload);
    }

    private static void printEvents(String label, InMemoryTransportService transport) {
        System.out.println(label + " events:");
        transport.events().forEach(event -> System.out.println(
                "  " + event.metadata().get("topic") + " -> " + event.message().getClass().getSimpleName()
                        + System.lineSeparator() + indent(event.message().toString())));
    }

    private static String indent(String value) {
        return value.replace(System.lineSeparator(), System.lineSeparator() + "  ");
    }
}
