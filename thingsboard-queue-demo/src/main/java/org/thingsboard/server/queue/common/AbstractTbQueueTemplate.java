package org.thingsboard.server.queue.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public abstract class AbstractTbQueueTemplate {
    protected static final String REQUEST_ID_HEADER = "requestId";
    protected static final String RESPONSE_TOPIC_HEADER = "responseTopic";
    protected static final String EXPIRE_TS_HEADER = "expireTs";

    protected static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    protected static UUID bytesToUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    protected static byte[] stringToBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    protected static String bytesToString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
