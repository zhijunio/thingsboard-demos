package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class TbActorMailbox implements TbActorCtx {
    private final DefaultTbActorSystem system;
    private final TbActorId actorId;
    private final TbActorRef parentRef;
    private final TbActor actor;
    private final ExecutorService executor;
    private final ConcurrentLinkedQueue<TbActorMsg> messages = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean destroying = new AtomicBoolean();
    private volatile TbActorStopReason stopReason;

    TbActorMailbox(DefaultTbActorSystem system, TbActorId actorId, TbActorRef parentRef,
                   TbActor actor, ExecutorService executor) {
        this.system = system;
        this.actorId = actorId;
        this.parentRef = parentRef;
        this.actor = actor;
        this.executor = executor;
    }

    void initActor() {
        executor.execute(() -> {
            try {
                actor.init(this);
                ready.set(true);
                tryProcess();
            } catch (TbActorException error) {
                stopReason = TbActorStopReason.INIT_FAILED;
                destroy(error);
            }
        });
    }

    private void tryProcess() {
        if (ready.get() && busy.compareAndSet(false, true)) {
            executor.execute(this::processMailbox);
        }
    }

    private void processMailbox() {
        boolean empty = false;
        for (int i = 0; i < system.settings().actorThroughput(); i++) {
            TbActorMsg message = messages.poll();
            if (message == null) {
                empty = true;
                break;
            }
            try {
                actor.process(message);
            } catch (Throwable error) {
                message.onTbActorStopped(TbActorStopReason.INIT_FAILED);
            }
        }
        if (empty) {
            busy.set(false);
            if (!messages.isEmpty()) {
                tryProcess();
            }
        } else {
            executor.execute(this::processMailbox);
        }
    }

    void destroy(Throwable cause) {
        if (!destroying.compareAndSet(false, true)) {
            return;
        }
        if (stopReason == null) {
            stopReason = TbActorStopReason.STOPPED;
        }
        executor.execute(() -> {
            ready.set(false);
            actor.destroy(stopReason, cause);
            TbActorMsg message;
            while ((message = messages.poll()) != null) {
                message.onTbActorStopped(stopReason);
            }
        });
    }

    @Override
    public TbActorId getActorId() {
        return actorId;
    }

    @Override
    public TbActorRef getParentRef() {
        return parentRef;
    }

    @Override
    public void tell(TbActorId target, TbActorMsg message) {
        system.tell(target, message);
    }

    @Override
    public void tell(TbActorMsg actorMsg) {
        if (destroying.get()) {
            actorMsg.onTbActorStopped(stopReason);
            return;
        }
        messages.add(actorMsg);
        tryProcess();
    }

    @Override
    public void tellWithHighPriority(TbActorMsg actorMsg) {
        tell(actorMsg);
    }

    @Override
    public void stop(TbActorId target) {
        system.stop(target);
    }

    @Override
    public TbActorRef getOrCreateChildActor(TbActorId childId, TbActorCreator creator) {
        return system.createChildActor("rule-dispatcher", creator, actorId);
    }
}
