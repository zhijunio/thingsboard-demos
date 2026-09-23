package org.example.thingsboard.cluster;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.cluster.CacheInvalidationEvent;
import org.thingsboard.server.cluster.ClusterMessage;
import org.thingsboard.server.cluster.ClusterNode;
import org.thingsboard.server.cluster.InMemoryClusterBus;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterDemoTest {
    @Test
    void messageIsRoutedToHashPartitionOwner() {
        InMemoryClusterBus bus = new InMemoryClusterBus();
        ClusterNode nodeA = new ClusterNode("node-a", bus);
        ClusterNode nodeB = new ClusterNode("node-b", bus);
        nodeA.start();
        nodeB.start();
        String entityId = nodeA.findEntityOwnedBy("node-b");
        var message = new ClusterMessage(UUID.randomUUID(), "TELEMETRY", entityId, "temperature=20", "node-a");
        AtomicReference<TbQueueMsgMetadata> callback = new AtomicReference<>();

        nodeA.clusterService().pushMsgToCore("tenant-1", entityId, message, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                callback.set(metadata);
            }

            @Override
            public void onFailure(Throwable error) {
                throw new AssertionError(error);
            }
        });

        assertEquals(1, nodeB.receivedMessages().size());
        assertEquals("TO_CORE", nodeB.receivedMessages().get(0).type());
        assertEquals("node-b", callback.get().targetNodeId());
        nodeA.close();
        nodeB.close();
    }

    @Test
    void broadcastReachesEveryActiveNode() {
        InMemoryClusterBus bus = new InMemoryClusterBus();
        ClusterNode nodeA = new ClusterNode("node-a", bus);
        ClusterNode nodeB = new ClusterNode("node-b", bus);
        nodeA.start();
        nodeB.start();

        nodeA.clusterService().broadcastCacheInvalidation("device-cache", "device-1");

        assertEquals(1, nodeA.cacheInvalidationEvents().size());
        assertEquals(1, nodeB.cacheInvalidationEvents().size());
        CacheInvalidationEvent event = nodeB.cacheInvalidationEvents().get(0);
        assertEquals("device-cache", event.cacheName());
        assertEquals("device-1", event.key());
        nodeA.close();
        nodeB.close();
    }

    @Test
    void unavailableTargetInvokesFailureCallback() {
        InMemoryClusterBus bus = new InMemoryClusterBus();
        ClusterNode nodeA = new ClusterNode("node-a", bus);
        ClusterNode nodeB = new ClusterNode("node-b", bus);
        nodeA.start();
        nodeB.start();
        String entityId = nodeA.findEntityOwnedBy("node-b");
        var stalePartition = nodeA.clusterService().resolve(ServiceType.TB_CORE, "tenant-1", entityId);
        nodeB.close();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        nodeA.clusterService().pushMsgToCore(stalePartition, UUID.randomUUID(),
                ClusterNode.message("TELEMETRY", entityId, "late"), new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        throw new AssertionError("send should fail");
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        failure.set(error);
                    }
                });

        assertNotNull(failure.get());
        assertInstanceOf(IllegalStateException.class, failure.get());
        assertTrue(failure.get().getMessage().contains("node-b"));
        nodeA.close();
    }

    @Test
    void topologyChangePublishesPartitionChange() {
        InMemoryClusterBus bus = new InMemoryClusterBus();
        ClusterNode nodeA = new ClusterNode("node-a", bus);
        nodeA.start();
        int before = nodeA.partitionChangeEvents().size();
        ClusterNode nodeB = new ClusterNode("node-b", bus);
        nodeB.start();

        assertTrue(nodeA.partitionChangeEvents().size() > before);
        assertTrue(nodeA.receivedEvents().stream().anyMatch(event -> event.getClass().getSimpleName()
                .equals("ClusterTopologyChangeEvent")));
        nodeA.close();
        nodeB.close();
    }
}
