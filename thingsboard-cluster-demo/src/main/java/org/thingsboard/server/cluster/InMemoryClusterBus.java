package org.thingsboard.server.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 代替 ThingsBoard 真实集群通知 topic 的进程内传输层。 */
public final class InMemoryClusterBus {
    private final Map<String, ClusterNode> nodes = new ConcurrentHashMap<>();

    public void register(ClusterNode node) {
        if (nodes.putIfAbsent(node.nodeId(), node) != null) {
            throw new IllegalArgumentException("Cluster node already registered: " + node.nodeId());
        }
        notifyTopologyChanged();
    }

    public void unregister(String nodeId) {
        nodes.remove(nodeId);
        notifyTopologyChanged();
    }

    public void send(String targetNodeId, ClusterMessage message) {
        ClusterNode target = nodes.get(targetNodeId);
        if (target == null) {
            throw new ClusterNodeUnavailableException(targetNodeId);
        }
        target.receiveMessage(message);
    }

    public void broadcast(ClusterEvent event) {
        nodes.values().forEach(node -> node.receiveEvent(event));
    }

    public Set<String> activeNodeIds() {
        return Set.copyOf(nodes.keySet());
    }

    private void notifyTopologyChanged() {
        Set<String> activeNodeIds = activeNodeIds();
        List<ClusterNode> activeNodes = new ArrayList<>(nodes.values());
        activeNodes.forEach(node -> node.onTopologyChanged(activeNodeIds));
    }
}
