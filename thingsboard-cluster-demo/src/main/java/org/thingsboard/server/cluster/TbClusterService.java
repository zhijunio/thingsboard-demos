package org.thingsboard.server.cluster;

import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;

import java.util.UUID;

/** 对齐 ThingsBoard 的集群服务边界，隐藏底层队列/服务发现实现。 */
public interface TbClusterService {
    void pushMsgToCore(TopicPartitionInfo tpi, UUID msgId, ClusterMessage msg, TbQueueCallback callback);

    void pushMsgToCore(String tenantId, String entityId, ClusterMessage msg, TbQueueCallback callback);

    void broadcastToCore(ClusterMessage msg);

    void pushNotificationToCore(String targetServiceId, ClusterMessage msg, TbQueueCallback callback);

    void broadcastCacheInvalidation(String cacheName, String key);

    TopicPartitionInfo resolve(ServiceType serviceType, String tenantId, String entityId);
}
