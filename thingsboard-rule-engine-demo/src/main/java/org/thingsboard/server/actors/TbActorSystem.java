package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.concurrent.ExecutorService;

public interface TbActorSystem {
    void createDispatcher(String id, ExecutorService executor);

    TbActorRef createRootActor(String dispatcherId, TbActorCreator creator);

    TbActorRef createChildActor(String dispatcherId, TbActorCreator creator, TbActorId parent);

    void tell(TbActorId target, TbActorMsg message);

    void stop(TbActorId actorId);

    void stop();
}
