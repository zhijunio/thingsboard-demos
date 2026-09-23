package org.thingsboard.server.cluster;

import java.util.UUID;

public record ClusterMessage(UUID id, String type, String entityId, String body, String sourceNodeId) {
}
