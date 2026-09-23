package org.example.thingsboard.ruleengine;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;

public record QueueToRuleEngineMsg(TbMsg msg) implements TbActorMsg {
    @Override
    public MsgType getMsgType() {
        return MsgType.QUEUE_TO_RULE_ENGINE_MSG;
    }
}
