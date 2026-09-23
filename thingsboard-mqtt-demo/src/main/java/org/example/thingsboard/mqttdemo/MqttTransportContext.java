package org.example.thingsboard.mqttdemo;

/** 对应源码中的 MqttTransportContext，负责持有 transport service。 */
public final class MqttTransportContext {

    private final TransportService transportService;

    public MqttTransportContext(TransportService transportService) {
        this.transportService = transportService;
    }

    public TransportService getTransportService() {
        return transportService;
    }
}
