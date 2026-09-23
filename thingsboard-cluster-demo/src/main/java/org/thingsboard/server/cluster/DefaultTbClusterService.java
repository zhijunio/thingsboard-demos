package org.thingsboard.server.cluster;

import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.HashPartitionService;

import java.util.UUID;

/** TbClusterService 的最小进程内实现；真实 TB 中这里会委托给 queue producer。 */
public final class DefaultTbClusterService implements TbClusterService {
    private final ClusterNode currentNode;
    private final InMemoryClusterBus bus;
    private final HashPartitionService partitionService;

    public DefaultTbClusterService(ClusterNode currentNode, InMemoryClusterBus bus,
                                   HashPartitionService partitionService) {
        this.currentNode = currentNode;
        this.bus = bus;
        this.partitionService = partitionService;
    }

    @Override
    public void pushMsgToCore(TopicPartitionInfo tpi, UUID msgId, ClusterMessage msg, TbQueueCallback callback) {
        send(tpi.getOwnerNodeId(), new ClusterMessage(msgId, "TO_CORE", msg.entityId(), msg.body(), currentNode.nodeId()), callback);
    }

    @Override
    public void pushMsgToCore(String tenantId, String entityId, ClusterMessage msg, TbQueueCallback callback) {
        pushMsgToCore(resolve(ServiceType.TB_CORE, tenantId, entityId), msg.id(), msg, callback);
    }

    @Override
    public void broadcastToCore(ClusterMessage msg) {
        bus.broadcast(new CoreBroadcastEvent(msg));
    }

    @Override
    public void pushNotificationToCore(String targetServiceId, ClusterMessage msg, TbQueueCallback callback) {
        send(targetServiceId, new ClusterMessage(msg.id(), "CORE_NOTIFICATION", msg.entityId(),
                msg.body(), currentNode.nodeId()), callback);
    }

    @Override
    public void broadcastCacheInvalidation(String cacheName, String key) {
        bus.broadcast(new CacheInvalidationEvent(cacheName, key, currentNode.nodeId()));
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, String tenantId, String entityId) {
        if (serviceType != ServiceType.TB_CORE) {
            throw new IllegalArgumentException("Demo only configures TB_CORE partitions");
        }
        return partitionService.resolve(tenantId, entityId);
    }

    private void send(String targetNodeId, ClusterMessage message, TbQueueCallback callback) {
        try {
            if (targetNodeId == null) {
                throw new ClusterNodeUnavailableException("<no owner>");
            }
            bus.send(targetNodeId, message);
            if (callback != null) {
                callback.onSuccess(new TbQueueMsgMetadata(targetNodeId));
            }
        } catch (RuntimeException error) {
            if (callback != null) {
                callback.onFailure(error);
            }
        }
    }

    public record CoreBroadcastEvent(ClusterMessage message) implements ClusterEvent {
    }
}
