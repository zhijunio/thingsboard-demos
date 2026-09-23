package org.example.thingsboard.cacheconsistency;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 对应 ThingsBoard VersionedCaffeineTbCache 的核心语义。
 *
 * 失效事件不是简单删除：版本号较旧的延迟事件不能覆盖较新的缓存状态，
 * tombstone 也保留版本号，避免旧数据回填。
 */
public final class VersionedLocalCache {
    private final Cache<String, Entry> cache = Caffeine.newBuilder().maximumSize(10_000).build();
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Set<Transaction>> transactions = new ConcurrentHashMap<>();

    public Optional<BusinessEntity> get(String key) {
        Entry entry = cache.getIfPresent(key);
        return entry == null || entry.value() == null ? Optional.empty() : Optional.of(entry.value());
    }

    public void put(BusinessEntity value) {
        lock.lock();
        try {
            Entry current = cache.getIfPresent(value.id());
            if (current == null || value.version() > current.version()
                    || current.value() == null && value.version() == current.version()) {
                failTransactions(value.id());
                cache.put(value.id(), new Entry(value.version(), value));
            }
        } finally {
            lock.unlock();
        }
    }

    public void evict(String key, long version) {
        lock.lock();
        try {
            Entry current = cache.getIfPresent(key);
            if (current == null || version > current.version()) {
                failTransactions(key);
                cache.put(key, new Entry(version, null));
            }
        } finally {
            lock.unlock();
        }
    }

    public BusinessEntity getAndPutInTransaction(String key, Supplier<BusinessEntity> dbCall) {
        Optional<BusinessEntity> cached = get(key);
        if (cached.isPresent()) {
            return cached.get();
        }

        Transaction transaction = newTransactionForKey(key);
        try {
            BusinessEntity value = dbCall.get();
            if (value == null) {
                transaction.rollback();
            } else {
                transaction.put(value);
                transaction.commit();
            }
            return value;
        } catch (RuntimeException error) {
            transaction.rollback();
            throw error;
        }
    }

    public Transaction newTransactionForKey(String key) {
        lock.lock();
        try {
            Transaction transaction = new Transaction(key);
            transactions.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(transaction);
            return transaction;
        } finally {
            lock.unlock();
        }
    }

    public long versionOf(String key) {
        Entry entry = cache.getIfPresent(key);
        return entry == null ? 0 : entry.version();
    }

    public boolean contains(String key) {
        return cache.getIfPresent(key) != null;
    }

    private void failTransactions(String key) {
        Set<Transaction> active = transactions.get(key);
        if (active != null) {
            active.forEach(transaction -> transaction.failed = true);
        }
    }

    private void removeTransaction(Transaction transaction) {
        Set<Transaction> active = transactions.get(transaction.key);
        if (active != null) {
            active.remove(transaction);
            if (active.isEmpty()) {
                transactions.remove(transaction.key, active);
            }
        }
    }

    public final class Transaction {
        private final String key;
        private BusinessEntity pendingValue;
        private boolean failed;
        private boolean active = true;

        private Transaction(String key) {
            this.key = key;
        }

        public void put(BusinessEntity value) {
            checkActive();
            if (!key.equals(value.id())) {
                throw new IllegalArgumentException("transaction key does not match value id");
            }
            pendingValue = value;
        }

        public boolean commit() {
            checkActive();
            lock.lock();
            try {
                boolean committed = !failed && pendingValue != null;
                if (committed) {
                    Entry current = cache.getIfPresent(key);
                    if (current == null || pendingValue.version() > current.version()
                            || current.value() == null && pendingValue.version() == current.version()) {
                        cache.put(key, new Entry(pendingValue.version(), pendingValue));
                    }
                }
                active = false;
                removeTransaction(this);
                return committed;
            } finally {
                lock.unlock();
            }
        }

        public void rollback() {
            if (!active) {
                return;
            }
            lock.lock();
            try {
                pendingValue = null;
                active = false;
                removeTransaction(this);
            } finally {
                lock.unlock();
            }
        }

        private void checkActive() {
            if (!active) {
                throw new IllegalStateException("cache transaction is already completed");
            }
        }
    }

    private record Entry(long version, BusinessEntity value) {
    }
}
