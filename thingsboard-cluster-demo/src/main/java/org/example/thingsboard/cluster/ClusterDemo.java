package org.example.thingsboard.cluster;

import org.thingsboard.server.cluster.CacheInvalidationEvent;
import org.thingsboard.server.cluster.ClusterMessage;
import org.thingsboard.server.cluster.ClusterNode;
import org.thingsboard.server.cluster.InMemoryClusterBus;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.UUID;

public final class ClusterDemo {
    private ClusterDemo() {
    }

    public static void main(String[] args) {
        InMemoryClusterBus bus = new InMemoryClusterBus();
        ClusterNode nodeA = new ClusterNode("node-a", bus);
        ClusterNode nodeB = new ClusterNode("node-b", bus);
        nodeA.start();
        nodeB.start();

        String entityId = nodeA.findEntityOwnedBy("node-b");
        var tpi = nodeA.clusterService().resolve(ServiceType.TB_CORE, "tenant-1", entityId);
        System.out.println("[Partition] " + entityId + " -> " + tpi);

        ClusterMessage toCore = new ClusterMessage(UUID.randomUUID(), "TELEMETRY", entityId,
                "temperature=23.5", nodeA.nodeId());
        nodeA.clusterService().pushMsgToCore(tpi, toCore.id(), toCore, printingCallback("定向消息"));
        System.out.println("[Forward] node-b received=" + nodeB.receivedMessages().size());

        nodeA.clusterService().broadcastToCore(ClusterNode.message("CONFIG_CHANGED", entityId, "profile=v2"));
        nodeA.clusterService().broadcastCacheInvalidation("device-cache", entityId);
        System.out.println("[Broadcast] node-a cache events=" + nodeA.cacheInvalidationEvents().size()
                + ", node-b cache events=" + nodeB.cacheInvalidationEvents().size());

        int nodeAPartitionEvents = nodeA.partitionChangeEvents().size();
        nodeB.close();
        nodeA.clusterService().pushMsgToCore(tpi, toCore.id(), toCore, printingCallback("节点下线后的发送"));
        System.out.println("[Topology] node-a partition changes="
                + (nodeA.partitionChangeEvents().size() - nodeAPartitionEvents));
        nodeA.close();
    }

    private static TbQueueCallback printingCallback(String name) {
        return new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                System.out.println("[Callback] " + name + " success -> " + metadata.targetNodeId());
            }

            @Override
            public void onFailure(Throwable error) {
                System.out.println("[Callback] " + name + " failure -> " + error.getMessage());
            }
        };
    }
}
