package org.thingsboard.server.queue.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Kafka Producer 的最小对应物，保留 ThingsBoard 的 key、value、headers 映射。
 */
public final class TbKafkaProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final KafkaProducer<String, byte[]> producer;
    private final String defaultTopic;

    public TbKafkaProducerTemplate(String bootstrapServers, String defaultTopic, String clientId) {
        this.defaultTopic = defaultTopic;
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(properties);
    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public void send(TopicPartitionInfo tpi, T message, TbQueueCallback callback) {
        String topic = tpi.getFullTopicName();
        List<Header> headers = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : message.getHeaders().getData().entrySet()) {
            headers.add(new RecordHeader(entry.getKey(), entry.getValue()));
        }
        ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(
                topic, tpi.getPartition(), message.getKey().toString(), message.getData(), headers);
        producer.send(record, (metadata, exception) -> {
            if (callback == null) {
                return;
            }
            if (exception == null) {
                callback.onSuccess(new KafkaTbQueueMsgMetadata(metadata.topic(), metadata.partition(), metadata.offset()));
            } else {
                callback.onFailure(exception);
            }
        });
    }

    @Override
    public void stop() {
        producer.close();
    }

    public record KafkaTbQueueMsgMetadata(String topic, int partition, long offset) implements TbQueueMsgMetadata {
    }
}
