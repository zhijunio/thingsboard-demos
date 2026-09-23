package org.example.thingsboard.cacheconsistency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 用内存 Map 模拟事务数据库；真实项目中对应 DAO/数据库提交。 */
public final class BusinessStore {
    private final Map<String, BusinessEntity> records = new ConcurrentHashMap<>();
    private volatile boolean failNextWrite;

    public void insert(BusinessEntity entity) {
        records.put(entity.id(), entity);
    }

    public BusinessEntity find(String id) {
        return records.get(id);
    }

    public Transaction beginTransaction() {
        return new Transaction();
    }

    public void failNextWrite() {
        failNextWrite = true;
    }

    public final class Transaction {
        private final Map<String, BusinessEntity> pendingWrites = new ConcurrentHashMap<>();
        private boolean active = true;

        public BusinessEntity updateName(String id, String name) {
            checkActive();
            synchronized (BusinessStore.this) {
                if (failNextWrite) {
                    failNextWrite = false;
                    throw new IllegalStateException("simulated database rollback");
                }
                BusinessEntity current = pendingWrites.get(id);
                if (current == null) {
                    current = records.get(id);
                }
                if (current == null) {
                    throw new IllegalArgumentException("entity not found: " + id);
                }
                BusinessEntity saved = new BusinessEntity(id, name, current.version() + 1);
                pendingWrites.put(id, saved);
                return saved;
            }
        }

        public void commit() {
            checkActive();
            synchronized (BusinessStore.this) {
                records.putAll(pendingWrites);
                pendingWrites.clear();
                active = false;
            }
        }

        public void rollback() {
            if (!active) {
                return;
            }
            pendingWrites.clear();
            active = false;
        }

        private void checkActive() {
            if (!active) {
                throw new IllegalStateException("business transaction is already completed");
            }
        }
    }
}
