package org.example.thingsboard.cacheconsistency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheConsistencyTest {
    @Test
    void committedWriteInvalidatesEveryNodeAndNextReadLoadsNewVersion() {
        BusinessStore store = new BusinessStore();
        CacheInvalidationBus bus = new CacheInvalidationBus();
        VersionedLocalCache aCache = new VersionedLocalCache();
        VersionedLocalCache bCache = new VersionedLocalCache();
        ConsistentEntityService a = new ConsistentEntityService(store, aCache, bus);
        ConsistentEntityService b = new ConsistentEntityService(store, bCache, bus);
        store.insert(new BusinessEntity("device-1", "v1", 1));
        a.get("device-1");
        b.get("device-1");

        a.updateName("device-1", "v2");

        assertEquals(java.util.Optional.empty(), aCache.get("device-1"));
        assertEquals(java.util.Optional.empty(), bCache.get("device-1"));
        assertEquals(2, aCache.versionOf("device-1"));
        assertEquals(2, bCache.versionOf("device-1"));
        assertEquals("v2", b.get("device-1").name());
        assertEquals(2, bCache.versionOf("device-1"));
    }

    @Test
    void delayedOlderInvalidationCannotEvictNewerCachedValue() {
        VersionedLocalCache cache = new VersionedLocalCache();
        cache.put(new BusinessEntity("device-1", "v2", 2));

        cache.evict("device-1", 1);

        assertEquals("v2", cache.get("device-1").orElseThrow().name());
        assertEquals(2, cache.versionOf("device-1"));
    }

    @Test
    void failedDatabaseWriteDoesNotPublishInvalidation() {
        BusinessStore store = new BusinessStore();
        CacheInvalidationBus bus = new CacheInvalidationBus();
        VersionedLocalCache cache = new VersionedLocalCache();
        ConsistentEntityService service = new ConsistentEntityService(store, cache, bus);
        store.insert(new BusinessEntity("device-1", "v1", 1));
        service.get("device-1");
        store.failNextWrite();

        assertThrows(IllegalStateException.class, () -> service.updateName("device-1", "v2"));
        assertEquals("v1", service.get("device-1").name());
        assertEquals(1, cache.versionOf("device-1"));
    }

    @Test
    void cacheTransactionRollbackDoesNotPublishPendingValue() {
        VersionedLocalCache cache = new VersionedLocalCache();
        VersionedLocalCache.Transaction transaction = cache.newTransactionForKey("device-1");

        transaction.put(new BusinessEntity("device-1", "pending", 1));
        transaction.rollback();

        assertEquals(java.util.Optional.empty(), cache.get("device-1"));
    }

    @Test
    void cacheEvictionFailsConcurrentReadTransaction() {
        VersionedLocalCache cache = new VersionedLocalCache();
        VersionedLocalCache.Transaction transaction = cache.newTransactionForKey("device-1");
        transaction.put(new BusinessEntity("device-1", "stale", 1));

        cache.evict("device-1", 2);

        assertEquals(false, transaction.commit());
        assertEquals(java.util.Optional.empty(), cache.get("device-1"));
        assertEquals(2, cache.versionOf("device-1"));
    }

    @Test
    void businessTransactionHidesPendingWriteUntilCommit() {
        BusinessStore store = new BusinessStore();
        store.insert(new BusinessEntity("device-1", "v1", 1));
        BusinessStore.Transaction transaction = store.beginTransaction();

        transaction.updateName("device-1", "v2");

        assertEquals("v1", store.find("device-1").name());
        transaction.commit();
        assertEquals("v2", store.find("device-1").name());
    }

    @Test
    void businessTransactionRollbackDiscardsPendingWrite() {
        BusinessStore store = new BusinessStore();
        store.insert(new BusinessEntity("device-1", "v1", 1));
        BusinessStore.Transaction transaction = store.beginTransaction();

        transaction.updateName("device-1", "v2");
        transaction.rollback();

        assertEquals("v1", store.find("device-1").name());
    }
}
