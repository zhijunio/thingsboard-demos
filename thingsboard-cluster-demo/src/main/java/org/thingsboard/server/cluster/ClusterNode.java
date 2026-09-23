package org.thingsboard.server.cluster;

import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClusterNode implements AutoCloseable {
    private final String nodeId;
    private final InMemoryClusterBus bus;
    private final HashPartitionService partitionService;
    private final DefaultTbClusterService clusterService;
    private final List<ClusterMessage> receivedMessages = new CopyOnWriteArrayList<>();
    private final List<ClusterEvent> receivedEvents = new CopyOnWriteArrayList<>();
    private volatile boolean started;

    public ClusterNode(String nodeId, InMemoryClusterBus bus) {
        this.nodeId = nodeId;
        this.bus = bus;
        this.partitionService = new HashPartitionService(nodeId, ServiceType.TB_CORE,
                "tb_core", "tb_core", 4);
        this.clusterService = new DefaultTbClusterService(this, bus, partitionService);
    }

    public void start() {
        if (!started) {
            started = true;
            bus.register(this);
        }
    }

    @Override
    public void close() {
        if (started) {
            started = false;
            bus.unregister(nodeId);
        }
    }

    void onTopologyChanged(Set<String> activeNodeIds) {
        PartitionChangeEvent partitionChange = partitionService.recalculatePartitions(activeNodeIds);
        if (partitionChange != null) {
            receiveEvent(partitionChange);
        }
        Set<QueueKey> queueKeys = Set.of(new QueueKey(ServiceType.TB_CORE, "tb_core"));
        receiveEvent(new ClusterTopologyChangeEvent(queueKeys, activeNodeIds));
        List<String> otherServices = new ArrayList<>(activeNodeIds);
        otherServices.remove(nodeId);
        receiveEvent(new ServiceListChangedEvent(nodeId, otherServices));
    }

    void receiveMessage(ClusterMessage message) {
        receivedMessages.add(message);
    }

    void receiveEvent(ClusterEvent event) {
        receivedEvents.add(event);
    }

    public String nodeId() {
        return nodeId;
    }

    public TbClusterService clusterService() {
        return clusterService;
    }

    public List<ClusterMessage> receivedMessages() {
        return List.copyOf(receivedMessages);
    }

    public List<ClusterEvent> receivedEvents() {
        return List.copyOf(receivedEvents);
    }

    public List<PartitionChangeEvent> partitionChangeEvents() {
        return receivedEvents.stream().filter(PartitionChangeEvent.class::isInstance)
                .map(PartitionChangeEvent.class::cast).toList();
    }

    public List<CacheInvalidationEvent> cacheInvalidationEvents() {
        return receivedEvents.stream().filter(CacheInvalidationEvent.class::isInstance)
                .map(CacheInvalidationEvent.class::cast).toList();
    }

    public String findEntityOwnedBy(String ownerNodeId) {
        for (int index = 0; index < 1000; index++) {
            String entityId = "device-" + index;
            if (ownerNodeId.equals(clusterService.resolve(ServiceType.TB_CORE, "tenant-1", entityId).getOwnerNodeId())) {
                return entityId;
            }
        }
        throw new IllegalStateException("No entity found for owner " + ownerNodeId);
    }

    public static ClusterMessage message(String type, String entityId, String body) {
        return new ClusterMessage(UUID.randomUUID(), type, entityId, body, "");
    }
}
