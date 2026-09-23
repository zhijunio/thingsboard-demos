package org.thingsboard.server.queue.memory;

import org.thingsboard.server.queue.TbQueueMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class DefaultInMemoryStorage implements InMemoryStorage {
    private final ConcurrentHashMap<String, BlockingQueue<TbQueueMsg>> storage = new ConcurrentHashMap<>();

    @Override
    public boolean put(String topic, TbQueueMsg message) {
        return storage.computeIfAbsent(topic, ignored -> new LinkedBlockingQueue<>()).offer(message);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TbQueueMsg> List<T> get(String topic) {
        BlockingQueue<TbQueueMsg> queue = storage.get(topic);
        if (queue == null) {
            return Collections.emptyList();
        }
        TbQueueMsg first = queue.poll();
        if (first == null) {
            return Collections.emptyList();
        }
        List<TbQueueMsg> messages = new ArrayList<>();
        messages.add(first);
        queue.drainTo(messages, 999);
        return (List<T>) (List<?>) messages;
    }

    @Override
    public int getLag(String topic) {
        BlockingQueue<TbQueueMsg> queue = storage.get(topic);
        return queue == null ? 0 : queue.size();
    }
}
