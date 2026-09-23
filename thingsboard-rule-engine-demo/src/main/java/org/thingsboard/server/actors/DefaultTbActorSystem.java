package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DefaultTbActorSystem implements TbActorSystem {
    private final Map<String, ExecutorService> dispatchers = new ConcurrentHashMap<>();
    private final Map<TbActorId, TbActorMailbox> actors = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final TbActorSystemSettings settings;

    public DefaultTbActorSystem(TbActorSystemSettings settings) {
        this.settings = settings;
        this.scheduler = Executors.newScheduledThreadPool(settings.schedulerPoolSize());
    }

    ScheduledExecutorService scheduler() {
        return scheduler;
    }

    TbActorSystemSettings settings() {
        return settings;
    }

    @Override
    public void createDispatcher(String id, ExecutorService executor) {
        if (dispatchers.putIfAbsent(id, executor) != null) {
            throw new IllegalArgumentException("dispatcher already exists: " + id);
        }
    }

    @Override
    public TbActorRef createRootActor(String dispatcherId, TbActorCreator creator) {
        return createActor(dispatcherId, creator, null);
    }

    @Override
    public TbActorRef createChildActor(String dispatcherId, TbActorCreator creator, TbActorId parent) {
        TbActorRef parentRef = actors.get(parent);
        if (parentRef == null) {
            throw new IllegalArgumentException("parent actor is not registered: " + parent);
        }
        return createActor(dispatcherId, creator, parentRef);
    }

    private TbActorRef createActor(String dispatcherId, TbActorCreator creator, TbActorRef parent) {
        ExecutorService executor = dispatchers.get(dispatcherId);
        if (executor == null) {
            throw new IllegalArgumentException("dispatcher is not registered: " + dispatcherId);
        }
        TbActorId actorId = creator.createActorId();
        TbActorMailbox existing = actors.get(actorId);
        if (existing != null) {
            return existing;
        }
        TbActorMailbox mailbox = new TbActorMailbox(this, actorId, parent, creator.createActor(), executor);
        TbActorMailbox winner = actors.putIfAbsent(actorId, mailbox);
        if (winner != null) {
            return winner;
        }
        mailbox.initActor();
        return mailbox;
    }

    @Override
    public void tell(TbActorId target, TbActorMsg message) {
        TbActorMailbox mailbox = actors.get(target);
        if (mailbox == null) {
            throw new IllegalArgumentException("actor is not registered: " + target);
        }
        mailbox.tell(message);
    }

    @Override
    public void stop(TbActorId actorId) {
        TbActorMailbox mailbox = actors.remove(actorId);
        if (mailbox != null) {
            mailbox.destroy(null);
        }
    }

    @Override
    public void stop() {
        actors.keySet().forEach(this::stop);
        dispatchers.values().forEach(executor -> {
            executor.shutdown();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });
        scheduler.shutdownNow();
    }
}
