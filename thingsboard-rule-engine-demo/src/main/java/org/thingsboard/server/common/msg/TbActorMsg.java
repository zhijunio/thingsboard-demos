package org.thingsboard.server.common.msg;

public interface TbActorMsg {

    MsgType getMsgType();

    default void onTbActorStopped(TbActorStopReason reason) {
    }
}
