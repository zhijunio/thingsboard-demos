package org.thingsboard.server.queue.discovery.event;

import org.thingsboard.server.cluster.ClusterEvent;

import java.util.List;

public record ServiceListChangedEvent(String currentServiceId, List<String> otherServices) implements ClusterEvent {
}
