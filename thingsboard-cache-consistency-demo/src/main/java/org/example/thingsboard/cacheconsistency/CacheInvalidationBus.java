package org.example.thingsboard.cacheconsistency;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** 模拟 ThingsBoard 通过事件/Queue 向其他节点传播缓存失效通知。 */
public final class CacheInvalidationBus {
    private final List<Consumer<CacheInvalidationEvent>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<CacheInvalidationEvent> listener) {
        listeners.add(listener);
    }

    public void publish(CacheInvalidationEvent event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}
