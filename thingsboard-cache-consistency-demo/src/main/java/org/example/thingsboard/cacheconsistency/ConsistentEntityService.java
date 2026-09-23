package org.example.thingsboard.cacheconsistency;

/**
 * 演示读缓存、写数据库、事务提交后发布失效事件的顺序。
 */
public final class ConsistentEntityService {
    private final BusinessStore store;
    private final VersionedLocalCache cache;
    private final CacheInvalidationBus bus;

    public ConsistentEntityService(BusinessStore store, VersionedLocalCache cache, CacheInvalidationBus bus) {
        this.store = store;
        this.cache = cache;
        this.bus = bus;
        bus.subscribe(event -> cache.evict(event.key(), event.version()));
    }

    public BusinessEntity get(String id) {
        return cache.getAndPutInTransaction(id, () -> store.find(id));
    }

    public BusinessEntity updateName(String id, String name) {
        BusinessStore.Transaction transaction = store.beginTransaction();
        try {
            BusinessEntity saved = transaction.updateName(id, name);
            transaction.commit();
            // 对应 @TransactionalEventListener：业务事务提交后才传播缓存失效事件。
            bus.publish(new CacheInvalidationEvent(saved.id(), saved.version()));
            return saved;
        } catch (RuntimeException error) {
            transaction.rollback();
            throw error;
        }
    }
}
