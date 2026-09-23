package org.thingsboard.server.queue.memory;

import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public final class InMemoryTbQueueConsumer<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    private final InMemoryStorage storage;
    private final String topic;
    private volatile Set<TopicPartitionInfo> partitions = Collections.emptySet();
    private volatile boolean stopped;
    private volatile boolean subscribed;

    public InMemoryTbQueueConsumer(InMemoryStorage storage, String topic) {
        this.storage = storage;
        this.topic = topic;
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
        this.subscribed = true;
        this.stopped = false;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public void unsubscribe() {
        stopped = true;
        subscribed = false;
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (!subscribed || stopped) {
            return Collections.emptyList();
        }
        List<T> messages = new ArrayList<>();
        for (TopicPartitionInfo partition : partitions) {
            try {
                messages.addAll(storage.<T>get(partition.getFullTopicName()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return messages;
            }
        }
        if (messages.isEmpty() && durationInMillis > 0) {
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Collections.emptyList();
            }
        }
        return messages;
    }

    @Override
    public void commit() {
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
