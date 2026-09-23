package org.thingsboard.server.queue.discovery.event;

import org.thingsboard.server.cluster.ClusterEvent;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;

public record ClusterTopologyChangeEvent(Set<QueueKey> queueKeys, Set<String> activeNodeIds) implements ClusterEvent {
}
