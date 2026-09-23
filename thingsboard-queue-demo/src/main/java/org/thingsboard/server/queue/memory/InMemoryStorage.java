package org.thingsboard.server.queue.memory;

import org.thingsboard.server.queue.TbQueueMsg;

import java.util.List;

public interface InMemoryStorage {
    boolean put(String topic, TbQueueMsg message);

    <T extends TbQueueMsg> List<T> get(String topic) throws InterruptedException;

    int getLag(String topic);
}
