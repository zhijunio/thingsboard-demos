package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class DefaultTbActorSystem implements TbActorSystem {
    private final ConcurrentMap<String, Dispatcher> dispatchers = new ConcurrentHashMap<>();
    private final ConcurrentMap<TbActorId, TbActorMailbox> actors = new ConcurrentHashMap<>();
    private final ConcurrentMap<TbActorId, ReentrantLock> actorCreationLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<TbActorId, Set<TbActorId>> parentChildMap = new ConcurrentHashMap<>();
    private final TbActorSystemSettings settings;
    private final ScheduledExecutorService scheduler;

    public DefaultTbActorSystem(TbActorSystemSettings settings) {
        this.settings = settings;
        this.scheduler = Executors.newScheduledThreadPool(settings.schedulerPoolSize());
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void createDispatcher(String dispatcherId, ExecutorService executor) {
        if (dispatchers.putIfAbsent(dispatcherId, new Dispatcher(dispatcherId, executor)) != null) {
            throw new IllegalArgumentException("Dispatcher already exists: " + dispatcherId);
        }
    }

    @Override
    public void destroyDispatcher(String dispatcherId) {
        Dispatcher dispatcher = dispatchers.remove(dispatcherId);
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher does not exist: " + dispatcherId);
        }
        dispatcher.executor().shutdownNow();
    }

    @Override
    public TbActorRef getActor(TbActorId actorId) {
        return actors.get(actorId);
    }

    @Override
    public TbActorRef createRootActor(String dispatcherId, TbActorCreator creator) {
        return createActor(dispatcherId, creator, null);
    }

    @Override
    public TbActorRef createChildActor(String dispatcherId, TbActorCreator creator, TbActorId parent) {
        return createActor(dispatcherId, creator, parent);
    }

    private TbActorRef createActor(String dispatcherId, TbActorCreator creator, TbActorId parent) {
        Dispatcher dispatcher = dispatchers.get(dispatcherId);
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher does not exist: " + dispatcherId);
        }
        TbActorId actorId = creator.createActorId();
        TbActorMailbox existing = actors.get(actorId);
        if (existing != null) {
            return existing;
        }
        ReentrantLock creationLock = actorCreationLocks.computeIfAbsent(actorId, ignored -> new ReentrantLock());
        creationLock.lock();
        try {
            existing = actors.get(actorId);
            if (existing != null) {
                return existing;
            }
            TbActorRef parentRef = parent == null ? null : actors.get(parent);
            if (parent != null && parentRef == null) {
                throw new TbActorNotRegisteredException(parent, "Parent actor is not registered: " + parent);
            }
            TbActorMailbox mailbox = new TbActorMailbox(this, settings, actorId, parentRef,
                    creator.createActor(), dispatcher);
            actors.put(actorId, mailbox);
            mailbox.initActor();
            if (parent != null) {
                parentChildMap.computeIfAbsent(parent, ignored -> ConcurrentHashMap.newKeySet()).add(actorId);
            }
            return mailbox;
        } finally {
            creationLock.unlock();
            actorCreationLocks.remove(actorId, creationLock);
        }
    }

    @Override
    public void tell(TbActorId target, TbActorMsg actorMsg) {
        tell(target, actorMsg, false);
    }

    @Override
    public void tellWithHighPriority(TbActorId target, TbActorMsg actorMsg) {
        tell(target, actorMsg, true);
    }

    private void tell(TbActorId target, TbActorMsg actorMsg, boolean highPriority) {
        TbActorMailbox mailbox = actors.get(target);
        if (mailbox == null) {
            throw new TbActorNotRegisteredException(target, "Actor is not registered: " + target);
        }
        if (highPriority) {
            mailbox.tellWithHighPriority(actorMsg);
        } else {
            mailbox.tell(actorMsg);
        }
    }

    @Override
    public void stop(TbActorRef actorRef) {
        stop(actorRef.getActorId());
    }

    @Override
    public void stop(TbActorId actorId) {
        Set<TbActorId> children = parentChildMap.remove(actorId);
        if (children != null) {
            children.forEach(this::stop);
        }
        parentChildMap.values().forEach(childIds -> childIds.remove(actorId));
        TbActorMailbox mailbox = actors.remove(actorId);
        if (mailbox != null) {
            mailbox.destroy(null);
        }
    }

    @Override
    public void stop() {
        actors.keySet().forEach(this::stop);
        dispatchers.values().forEach(dispatcher -> {
            dispatcher.executor().shutdown();
            try {
                dispatcher.executor().awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });
        scheduler.shutdownNow();
    }

    @Override
    public void broadcastToChildren(TbActorId parent, TbActorMsg msg) {
        broadcastToChildren(parent, id -> true, msg, false);
    }

    @Override
    public void broadcastToChildren(TbActorId parent, TbActorMsg msg, boolean highPriority) {
        broadcastToChildren(parent, id -> true, msg, highPriority);
    }

    @Override
    public void broadcastToChildren(TbActorId parent, Predicate<TbActorId> childFilter, TbActorMsg msg) {
        broadcastToChildren(parent, childFilter, msg, false);
    }

    private void broadcastToChildren(TbActorId parent, Predicate<TbActorId> childFilter,
                                     TbActorMsg msg, boolean highPriority) {
        Set<TbActorId> children = parentChildMap.get(parent);
        if (children != null) {
            children.stream().filter(childFilter).forEach(child -> {
                try {
                    tell(child, msg, highPriority);
                } catch (TbActorNotRegisteredException ignored) {
                    // A child can stop while a broadcast is in progress.
                }
            });
        }
    }

    @Override
    public List<TbActorId> filterChildren(TbActorId parent, Predicate<TbActorId> childFilter) {
        Set<TbActorId> children = parentChildMap.get(parent);
        return children == null ? Collections.emptyList() : children.stream()
                .filter(childFilter)
                .collect(Collectors.toList());
    }
}
