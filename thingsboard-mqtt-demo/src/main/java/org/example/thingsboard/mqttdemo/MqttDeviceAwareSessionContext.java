package org.example.thingsboard.mqttdemo;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对应源码中的 MqttDeviceAwareSessionContext。
 *
 * 真实实现还维护设备身份、订阅 QoS 和 channel；这里仅保留 topic 分发所需的状态。
 */
public class MqttDeviceAwareSessionContext {

    private final UUID sessionId = UUID.randomUUID();
    private final AtomicInteger msgIdSeq = new AtomicInteger();

    public UUID getSessionId() {
        return sessionId;
    }

    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    public MqttQoS getQoSForTopic(String topic) {
        return MqttQoS.AT_LEAST_ONCE;
    }
}
