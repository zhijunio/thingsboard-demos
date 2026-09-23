package org.example.thingsboard.mqttdemo;

import com.google.protobuf.Message;

import java.util.Map;

/**
 * 对应源码中的 org.thingsboard.server.common.transport.TransportService。
 * 真实实现会继续进入 transport service、actor 和 queue；示例只记录入站事件。
 */
public interface TransportService {

    void process(DeviceTransportType transportType,
                 Message request,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    void process(DeviceSessionCtx session, Message message, Map<String, String> metadata);
}
