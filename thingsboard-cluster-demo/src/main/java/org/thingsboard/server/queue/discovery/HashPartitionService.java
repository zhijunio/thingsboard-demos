package org.thingsboard.server.queue.discovery;

import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** 用稳定的 hash(partition) + 节点排序，演示 HashPartitionService 的核心职责。 */
public final class HashPartitionService {
    private final String currentNodeId;
    private final ServiceType serviceType;
    private final QueueKey queueKey;
    private final String topic;
    private final int partitionCount;
    private Set<Integer> myPartitions = Set.of();
    private List<String> activeNodeIds = List.of();

    public HashPartitionService(String currentNodeId, ServiceType serviceType,
                                String queueName, String topic, int partitionCount) {
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("partitionCount must be positive");
        }
        this.currentNodeId = currentNodeId;
        this.serviceType = serviceType;
        this.queueKey = new QueueKey(serviceType, queueName);
        this.topic = topic;
        this.partitionCount = partitionCount;
    }

    public synchronized PartitionChangeEvent recalculatePartitions(Set<String> nodeIds) {
        Set<Integer> oldPartitions = myPartitions;
        List<String> oldNodes = activeNodeIds;
        activeNodeIds = nodeIds.stream().sorted().toList();
        Set<Integer> newPartitions = new HashSet<>();
        for (int partition = 0; partition < partitionCount; partition++) {
            if (currentNodeId.equals(ownerOf(partition))) {
                newPartitions.add(partition);
            }
        }
        myPartitions = Set.copyOf(newPartitions);
        if (oldPartitions.equals(myPartitions) && oldNodes.equals(activeNodeIds)) {
            return null;
        }
        return new PartitionChangeEvent(serviceType,
                Map.of(queueKey, toTopicPartitions(myPartitions)),
                Map.of(queueKey, toTopicPartitions(oldPartitions, oldNodes)));
    }

    public synchronized TopicPartitionInfo resolve(String tenantId, String entityId) {
        int partition = resolvePartitionIndex(entityId, partitionCount);
        String owner = ownerOf(partition);
        return new TopicPartitionInfo(topic, tenantId, partition, owner,
                currentNodeId.equals(owner));
    }

    public synchronized int resolvePartitionIndex(String key, int partitions) {
        return Math.floorMod(key.hashCode(), partitions);
    }

    public synchronized Set<Integer> getMyPartitions() {
        return myPartitions;
    }

    public synchronized List<String> getActiveNodeIds() {
        return activeNodeIds;
    }

    private String ownerOf(int partition) {
        if (activeNodeIds.isEmpty()) {
            return null;
        }
        return activeNodeIds.get(partition % activeNodeIds.size());
    }

    private Set<TopicPartitionInfo> toTopicPartitions(Set<Integer> partitions) {
        return toTopicPartitions(partitions, activeNodeIds);
    }

    private Set<TopicPartitionInfo> toTopicPartitions(Set<Integer> partitions, List<String> nodes) {
        Set<TopicPartitionInfo> result = new TreeSet<>((left, right) -> {
            int comparison = Integer.compare(left.getPartition(), right.getPartition());
            return comparison == 0 ? left.getFullTopicName().compareTo(right.getFullTopicName()) : comparison;
        });
        for (int partition : partitions) {
            String owner = nodes.isEmpty() ? null : nodes.get(partition % nodes.size());
            result.add(new TopicPartitionInfo(topic, null, partition, owner,
                    currentNodeId.equals(owner)));
        }
        return Collections.unmodifiableSet(result);
    }
}
