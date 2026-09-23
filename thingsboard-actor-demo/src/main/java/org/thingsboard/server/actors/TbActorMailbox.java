package org.thingsboard.server.actors;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class TbActorMailbox implements TbActorCtx {
    private final TbActorSystem system;
    private final TbActorSystemSettings settings;
    private final TbActorId selfId;
    private final TbActorRef parentRef;
    private final TbActor actor;
    private final Dispatcher dispatcher;
    private final ConcurrentLinkedQueue<TbActorMsg> highPriorityMsgs = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TbActorMsg> normalPriorityMsgs = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean destroyInProgress = new AtomicBoolean();
    private volatile TbActorStopReason stopReason;

    public TbActorMailbox(TbActorSystem system, TbActorSystemSettings settings, TbActorId selfId,
                          TbActorRef parentRef, TbActor actor, Dispatcher dispatcher) {
        this.system = system;
        this.settings = settings;
        this.selfId = selfId;
        this.parentRef = parentRef;
        this.actor = actor;
        this.dispatcher = dispatcher;
    }

    public void initActor() {
        dispatcher.executor().execute(() -> tryInit(1));
    }

    private void tryInit(int attempt) {
        try {
            if (!destroyInProgress.get()) {
                actor.init(this);
                if (!destroyInProgress.get()) {
                    ready.set(true);
                    tryProcessQueue(false);
                }
            }
        } catch (Throwable error) {
            int nextAttempt = attempt + 1;
            InitFailureStrategy strategy = actor.onInitFailure(attempt, error);
            if (strategy.isStop() || settings.maxActorInitAttempts() > 0
                    && nextAttempt > settings.maxActorInitAttempts()) {
                stopReason = TbActorStopReason.INIT_FAILED;
                destroy(error);
            } else if (strategy.getRetryDelay() > 0) {
                system.getScheduler().schedule(
                        () -> dispatcher.executor().execute(() -> tryInit(nextAttempt)),
                        strategy.getRetryDelay(), TimeUnit.MILLISECONDS);
            } else {
                dispatcher.executor().execute(() -> tryInit(nextAttempt));
            }
        }
    }

    private void enqueue(TbActorMsg msg, boolean highPriority) {
        if (destroyInProgress.get()) {
            msg.onTbActorStopped(stopReason);
            return;
        }
        if (highPriority) {
            highPriorityMsgs.add(msg);
        } else {
            normalPriorityMsgs.add(msg);
        }
        tryProcessQueue(true);
    }

    private void tryProcessQueue(boolean newMessage) {
        if (!ready.get()) {
            return;
        }
        if (newMessage || !highPriorityMsgs.isEmpty() || !normalPriorityMsgs.isEmpty()) {
            if (busy.compareAndSet(false, true)) {
                dispatcher.executor().execute(this::processMailbox);
            }
        }
    }

    private void processMailbox() {
        boolean empty = false;
        for (int i = 0; i < settings.actorThroughput(); i++) {
            TbActorMsg msg = highPriorityMsgs.poll();
            if (msg == null) {
                msg = normalPriorityMsgs.poll();
            }
            if (msg == null) {
                empty = true;
                break;
            }
            try {
                actor.process(msg);
            } catch (Throwable error) {
                if (actor.onProcessFailure(msg, error).isStop()) {
                    system.stop(selfId);
                    return;
                }
            }
        }
        if (empty) {
            busy.set(false);
            tryProcessQueue(false);
        } else {
            dispatcher.executor().execute(this::processMailbox);
        }
    }

    public void destroy(Throwable cause) {
        if (stopReason == null) {
            stopReason = TbActorStopReason.STOPPED;
        }
        if (!destroyInProgress.compareAndSet(false, true)) {
            return;
        }
        dispatcher.executor().execute(() -> {
            ready.set(false);
            try {
                actor.destroy(stopReason, cause);
            } catch (Throwable ignored) {
                // Cleanup must continue so queued messages receive stop callbacks.
            }
            drain(highPriorityMsgs);
            drain(normalPriorityMsgs);
        });
    }

    private void drain(ConcurrentLinkedQueue<TbActorMsg> messages) {
        TbActorMsg msg;
        while ((msg = messages.poll()) != null) {
            msg.onTbActorStopped(stopReason);
        }
    }

    @Override
    public TbActorId getActorId() {
        return selfId;
    }

    @Override
    public TbActorId getSelf() {
        return selfId;
    }

    @Override
    public TbActorRef getParentRef() {
        return parentRef;
    }

    @Override
    public void tell(TbActorMsg actorMsg) {
        enqueue(actorMsg, false);
    }

    @Override
    public void tellWithHighPriority(TbActorMsg actorMsg) {
        enqueue(actorMsg, true);
    }

    @Override
    public void tell(TbActorId target, TbActorMsg msg) {
        system.tell(target, msg);
    }

    @Override
    public void stop(TbActorId target) {
        system.stop(target);
    }

    @Override
    public TbActorRef getOrCreateChildActor(TbActorId actorId, Supplier<String> dispatcher,
                                            Supplier<TbActorCreator> creator, Supplier<Boolean> createCondition) {
        TbActorRef actorRef = system.getActor(actorId);
        return actorRef == null && createCondition.get()
                ? system.createChildActor(dispatcher.get(), creator.get(), selfId)
                : actorRef;
    }

    @Override
    public void broadcastToChildren(TbActorMsg msg) {
        system.broadcastToChildren(selfId, msg);
    }

    @Override
    public void broadcastToChildren(TbActorMsg msg, boolean highPriority) {
        system.broadcastToChildren(selfId, msg, highPriority);
    }

    @Override
    public void broadcastToChildrenByType(TbActorMsg msg, EntityType entityType) {
        broadcastToChildren(msg, id -> entityType.equals(id.getEntityType()));
    }

    @Override
    public void broadcastToChildren(TbActorMsg msg, Predicate<TbActorId> childFilter) {
        system.broadcastToChildren(selfId, childFilter, msg);
    }

    @Override
    public List<TbActorId> filterChildren(Predicate<TbActorId> childFilter) {
        return system.filterChildren(selfId, childFilter);
    }
}
