package org.thingsboard.server.queue.kafka;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Parser;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.io.IOException;

public final class ProtobufKafkaDecoder<T extends GeneratedMessageV3>
        implements TbKafkaDecoder<TbProtoQueueMsg<T>> {
    private final Parser<T> parser;

    public ProtobufKafkaDecoder(Parser<T> parser) {
        this.parser = parser;
    }

    @Override
    public TbProtoQueueMsg<T> decode(TbQueueMsg message) throws IOException {
        return new TbProtoQueueMsg<>(message.getKey(), parser.parseFrom(message.getData()), message.getHeaders());
    }
}
