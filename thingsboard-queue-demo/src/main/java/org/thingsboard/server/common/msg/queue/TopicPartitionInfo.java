package org.thingsboard.server.common.msg.queue;

import java.util.Objects;

/** 本示例保留 TopicPartitionInfo 的 topic/partition/fullTopicName 语义。 */
public final class TopicPartitionInfo {
    private final String topic;
    private final Integer partition;
    private final String fullTopicName;

    public TopicPartitionInfo(String topic, Integer partition) {
        this.topic = topic;
        this.partition = partition;
        this.fullTopicName = partition == null ? topic : topic + "." + partition;
    }

    public String getTopic() {
        return topic;
    }

    public Integer getPartition() {
        return partition;
    }

    public String getFullTopicName() {
        return fullTopicName;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TopicPartitionInfo that)) {
            return false;
        }
        return Objects.equals(fullTopicName, that.fullTopicName);
    }

    @Override
    public int hashCode() {
        return fullTopicName.hashCode();
    }
}
