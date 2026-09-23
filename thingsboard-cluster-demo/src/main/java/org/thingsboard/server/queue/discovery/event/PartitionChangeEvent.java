package org.thingsboard.server.queue.discovery.event;

import org.thingsboard.server.cluster.ClusterEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Map;
import java.util.Set;

public record PartitionChangeEvent(ServiceType serviceType,
                                   Map<QueueKey, Set<TopicPartitionInfo>> newPartitions,
                                   Map<QueueKey, Set<TopicPartitionInfo>> oldPartitions) implements ClusterEvent {
    public Set<TopicPartitionInfo> getPartitions() {
        return newPartitions.values().stream().flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
    }
}
