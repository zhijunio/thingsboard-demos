package org.thingsboard.server.queue.memory;

import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;

public final class InMemoryTbQueueProducer<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final InMemoryStorage storage;
    private final String defaultTopic;

    public InMemoryTbQueueProducer(InMemoryStorage storage, String defaultTopic) {
        this.storage = storage;
        this.defaultTopic = defaultTopic;
    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public void send(TopicPartitionInfo tpi, T message, TbQueueCallback callback) {
        if (storage.put(tpi.getFullTopicName(), message)) {
            if (callback != null) {
                callback.onSuccess(null);
            }
        } else if (callback != null) {
            callback.onFailure(new IllegalStateException("failed to put message"));
        }
    }

    @Override
    public void stop() {
    }
}
