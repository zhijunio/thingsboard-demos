package org.thingsboard.server.cluster;

public record CacheInvalidationEvent(String cacheName, String key, String sourceNodeId) implements ClusterEvent {
}
