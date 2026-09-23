package org.thingsboard.server.queue.discovery;

import org.thingsboard.server.common.msg.queue.ServiceType;

public record QueueKey(ServiceType type, String queueName, String tenantId) {
    public QueueKey(ServiceType type, String queueName) {
        this(type, queueName, null);
    }

    @Override
    public String toString() {
        return type + ":" + queueName + (tenantId == null ? "" : ":" + tenantId);
    }
}
