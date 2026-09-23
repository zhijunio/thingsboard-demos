package org.thingsboard.server.queue.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Kafka Consumer 的最小对应物，使用 Kafka consumer group 管理 offset。 */
public final class TbKafkaQueueConsumer<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    private final KafkaConsumer<String, byte[]> consumer;
    private final TbKafkaDecoder<T> decoder;
    private final String topic;
    private volatile Set<TopicPartitionInfo> partitions = Collections.emptySet();
    private volatile boolean stopped;
    private volatile boolean subscribed;

    public TbKafkaQueueConsumer(String bootstrapServers, String topic, String groupId,
                                String clientId, TbKafkaDecoder<T> decoder) {
        this.topic = topic;
        this.decoder = decoder;
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        this.consumer = new KafkaConsumer<>(properties);
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {
        subscribe(Collections.singleton(new TopicPartitionInfo(topic, null)));
    }

    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.partitions = new LinkedHashSet<>(partitions);
        Collection<String> topics = this.partitions.stream().map(TopicPartitionInfo::getFullTopicName).toList();
        consumer.subscribe(topics);
        subscribed = true;
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (!subscribed || stopped) {
            return Collections.emptyList();
        }
        List<T> messages = new ArrayList<>();
        for (ConsumerRecord<String, byte[]> record : consumer.poll(Duration.ofMillis(durationInMillis))) {
            try {
                messages.add(decoder.decode(new KafkaTbQueueMsg(record)));
            } catch (Exception e) {
                throw new IllegalStateException("failed to decode Kafka record", e);
            }
        }
        return messages;
    }

    @Override
    public void commit() {
        if (subscribed && !stopped) {
            consumer.commitSync();
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public void unsubscribe() {
        stopped = true;
        subscribed = false;
        consumer.unsubscribe();
        consumer.close();
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public Set<TopicPartitionInfo> getPartitions() {
        return partitions;
    }

    @Override
    public List<String> getFullTopicNames() {
        return partitions.stream().map(TopicPartitionInfo::getFullTopicName).toList();
    }
}
