package org.thingsboard.server.queue.common;

import com.google.protobuf.GeneratedMessageV3;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;

import java.util.UUID;

/** 对应 ThingsBoard common/queue 中的 TbProtoQueueMsg。 */
public final class TbProtoQueueMsg<T extends GeneratedMessageV3> implements TbQueueMsg {
    private final UUID key;
    private final T value;
    private final TbQueueMsgHeaders headers;

    public TbProtoQueueMsg(UUID key, T value) {
        this(key, value, new DefaultTbQueueMsgHeaders());
    }

    public TbProtoQueueMsg(UUID key, T value, TbQueueMsgHeaders headers) {
        this.key = key;
        this.value = value;
        this.headers = headers;
    }

    @Override
    public UUID getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    @Override
    public TbQueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return value == null ? null : value.toByteArray();
    }
}
