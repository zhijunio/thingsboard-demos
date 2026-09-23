package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

public interface TbActor {
    boolean process(TbActorMsg message);

    default void init(TbActorCtx ctx) throws TbActorException {
    }

    default void destroy(TbActorStopReason reason, Throwable cause) {
    }
}
