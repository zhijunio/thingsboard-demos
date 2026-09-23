package org.example.thingsboard.devicesession;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

public record DeviceRpcRequestMsg(ServerSideRpcRequest request) implements TbActorMsg {
    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG;
    }
}
