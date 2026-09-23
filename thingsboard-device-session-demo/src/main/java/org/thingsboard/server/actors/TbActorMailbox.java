package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

final class TbActorMailbox implements TbActorCtx {
    private final TbActorId actorId;
    private final TbActor actor;
    private final ExecutorService executor;
    private final ConcurrentLinkedQueue<TbActorMsg> messages = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    TbActorMailbox(TbActorId actorId, TbActor actor, ExecutorService executor) {
        this.actorId = actorId;
        this.actor = actor;
        this.executor = executor;
    }

    void initActor() {
        executor.execute(() -> {
            try {
                actor.init(this);
                ready.set(true);
                processNext();
            } catch (TbActorException error) {
                destroy(TbActorStopReason.INIT_FAILED, error);
            }
        });
    }

    @Override
    public TbActorId getActorId() {
        return actorId;
    }

    @Override
    public void tell(TbActorMsg message) {
        if (stopped.get()) {
            message.onTbActorStopped(TbActorStopReason.STOPPED);
            return;
        }
        messages.add(message);
        processNext();
    }

    private void processNext() {
        if (ready.get() && busy.compareAndSet(false, true)) {
            executor.execute(() -> {
                TbActorMsg message = messages.poll();
                if (message != null && !stopped.get()) {
                    actor.process(message);
                }
                busy.set(false);
                if (!messages.isEmpty()) {
                    processNext();
                }
            });
        }
    }

    void destroy() {
        destroy(TbActorStopReason.STOPPED, null);
    }

    private void destroy(TbActorStopReason reason, Throwable cause) {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        executor.execute(() -> {
            ready.set(false);
            actor.destroy(reason, cause);
            TbActorMsg message;
            while ((message = messages.poll()) != null) {
                message.onTbActorStopped(reason);
            }
        });
    }
}
