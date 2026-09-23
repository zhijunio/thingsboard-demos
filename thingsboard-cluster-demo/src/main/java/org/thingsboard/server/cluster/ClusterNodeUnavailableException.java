package org.thingsboard.server.cluster;

public final class ClusterNodeUnavailableException extends IllegalStateException {
    public ClusterNodeUnavailableException(String nodeId) {
        super("Cluster node is unavailable: " + nodeId);
    }
}
