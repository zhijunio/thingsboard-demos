package org.thingsboard.server.common.msg.queue;

import java.util.Objects;

/** 保留 ThingsBoard 中 topic、partition 和本节点归属的语义。 */
public final class TopicPartitionInfo {
    private final String topic;
    private final String tenantId;
    private final Integer partition;
    private final String ownerNodeId;
    private final boolean myPartition;
    private final String fullTopicName;

    public TopicPartitionInfo(String topic, String tenantId, Integer partition,
                              String ownerNodeId, boolean myPartition) {
        this.topic = topic;
        this.tenantId = tenantId;
        this.partition = partition;
        this.ownerNodeId = ownerNodeId;
        this.myPartition = myPartition;
        String tenantSuffix = tenantId == null ? "" : ".isolated." + tenantId;
        this.fullTopicName = topic + tenantSuffix + (partition == null ? "" : "." + partition);
    }

    public String getTopic() {
        return topic;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Integer getPartition() {
        return partition;
    }

    public String getOwnerNodeId() {
        return ownerNodeId;
    }

    public boolean isMyPartition() {
        return myPartition;
    }

    public String getFullTopicName() {
        return fullTopicName;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TopicPartitionInfo that)) {
            return false;
        }
        return Objects.equals(partition, that.partition)
                && Objects.equals(fullTopicName, that.fullTopicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullTopicName, partition);
    }

    @Override
    public String toString() {
        return fullTopicName + " -> " + ownerNodeId;
    }
}
