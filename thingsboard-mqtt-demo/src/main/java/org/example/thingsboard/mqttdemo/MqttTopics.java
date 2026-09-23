package org.example.thingsboard.mqttdemo;

/** 精简自 org.thingsboard.server.common.data.device.profile.MqttTopics。 */
public final class MqttTopics {

    public static final String BASE_DEVICE_API_TOPIC = "v1/devices/me";
    public static final String DEVICE_TELEMETRY_TOPIC = BASE_DEVICE_API_TOPIC + "/telemetry";
    public static final String DEVICE_ATTRIBUTES_TOPIC = BASE_DEVICE_API_TOPIC + "/attributes";
    public static final String DEVICE_RPC_RESPONSE_TOPIC = BASE_DEVICE_API_TOPIC + "/rpc/response/";
    public static final String DEVICE_RPC_REQUESTS_TOPIC = BASE_DEVICE_API_TOPIC + "/rpc/request/";
    public static final String DEVICE_RPC_REQUESTS_SUB_TOPIC = DEVICE_RPC_REQUESTS_TOPIC + "+";
    public static final String DEVICE_PROVISION_REQUEST_TOPIC = "/provision/request";

    private MqttTopics() {
    }
}
