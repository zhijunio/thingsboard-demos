package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DefaultTbActorSystem implements TbActorSystem {
    private final Map<TbActorId, TbActorMailbox> actors = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public TbActorRef createRootActor(TbActorCreator creator) {
        TbActorId actorId = creator.createActorId();
        return actors.computeIfAbsent(actorId, ignored -> {
            TbActorMailbox mailbox = new TbActorMailbox(actorId, creator.createActor(), executor);
            mailbox.initActor();
            return mailbox;
        });
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
    public void stop(TbActorId target) {
        TbActorMailbox mailbox = actors.remove(target);
        if (mailbox != null) {
            mailbox.destroy();
        }
    }

    @Override
    public void stop() {
        actors.keySet().forEach(this::stop);
        executor.shutdownNow();
    }
}
