package org.example.thingsboard.cacheconsistency;

public final class CacheConsistencyDemo {
    private CacheConsistencyDemo() {
    }

    public static void main(String[] args) {
        BusinessStore store = new BusinessStore();
        CacheInvalidationBus bus = new CacheInvalidationBus();
        VersionedLocalCache nodeACache = new VersionedLocalCache();
        VersionedLocalCache nodeBCache = new VersionedLocalCache();
        ConsistentEntityService nodeA = new ConsistentEntityService(store, nodeACache, bus);
        ConsistentEntityService nodeB = new ConsistentEntityService(store, nodeBCache, bus);

        store.insert(new BusinessEntity("device-1", "old-name", 1));
        nodeA.get("device-1");
        nodeB.get("device-1");
        System.out.println("[初始缓存] A=" + nodeACache.versionOf("device-1")
                + ", B=" + nodeBCache.versionOf("device-1"));

        nodeA.updateName("device-1", "new-name");
        System.out.println("[提交后失效] A value=" + nodeACache.get("device-1")
                + ", B value=" + nodeBCache.get("device-1")
                + ", version=" + nodeACache.versionOf("device-1"));
        System.out.println("[重新读取] B=" + nodeB.get("device-1"));

        bus.publish(new CacheInvalidationEvent("device-1", 1));
        System.out.println("[旧事件] B version=" + nodeBCache.versionOf("device-1")
                + ", name=" + nodeB.get("device-1").name());

        store.failNextWrite();
        try {
            nodeA.updateName("device-1", "must-not-publish");
        } catch (IllegalStateException error) {
            System.out.println("[回滚] " + error.getMessage());
        }
        System.out.println("完成：提交后失效、旧事件保护、失败写入不发布失效事件。");
    }
}
