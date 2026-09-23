package org.thingsboard.server.queue.kafka;

import org.thingsboard.server.queue.TbQueueMsg;

import java.io.IOException;

public interface TbKafkaDecoder<T> {
    T decode(TbQueueMsg message) throws IOException;
}
