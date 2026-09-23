package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

public interface TbActorCtx extends TbActorRef {

    TbActorRef getParentRef();

    void tell(TbActorId target, TbActorMsg message);

    void stop(TbActorId target);

    TbActorRef getOrCreateChildActor(TbActorId actorId, TbActorCreator creator);
}
