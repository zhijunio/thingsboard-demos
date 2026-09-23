package org.thingsboard.server.queue.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;
import org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders;

import java.util.UUID;

/** Kafka record 到 ThingsBoard TbQueueMsg 的适配层。 */
public final class KafkaTbQueueMsg implements TbQueueMsg {
    private final UUID key;
    private final TbQueueMsgHeaders headers;
    private final byte[] data;

    public KafkaTbQueueMsg(ConsumerRecord<String, byte[]> record) {
        this.key = parseKey(record.key());
        DefaultTbQueueMsgHeaders messageHeaders = new DefaultTbQueueMsgHeaders();
        record.headers().forEach(header -> messageHeaders.put(header.key(), header.value()));
        this.headers = messageHeaders;
        this.data = record.value();
    }

    private static UUID parseKey(String key) {
        if (key != null) {
            try {
                return UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                // Kafka 消息可能使用业务 key，沿用 ThingsBoard 的随机 key 回退行为。
            }
        }
        return UUID.randomUUID();
    }

    @Override
    public UUID getKey() {
        return key;
    }

    @Override
    public TbQueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
