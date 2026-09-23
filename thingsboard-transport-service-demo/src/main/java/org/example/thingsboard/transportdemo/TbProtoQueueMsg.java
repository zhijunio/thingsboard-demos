package org.example.thingsboard.transportdemo;

import com.google.protobuf.GeneratedMessageV3;

import java.util.UUID;

/** ThingsBoard 队列消息的最小对应物：路由 key + Protobuf value。 */
public final class TbProtoQueueMsg<T extends GeneratedMessageV3> {
    private final UUID key;
    private final T value;

    public TbProtoQueueMsg(UUID key, T value) {
        this.key = key;
        this.value = value;
    }

    public UUID getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public byte[] getData() {
        return value == null ? null : value.toByteArray();
    }
}
