package org.thingsboard.server.actors;

public interface TbActorSystem {
    TbActorRef createRootActor(TbActorCreator creator);

    void tell(TbActorId target, org.thingsboard.server.common.msg.TbActorMsg message);

    void stop(TbActorId target);

    void stop();
}
