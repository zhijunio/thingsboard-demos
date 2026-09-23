package org.thingsboard.server.queue.kafka;

public interface TbKafkaEncoder<T> {
    byte[] encode(T value);
}
