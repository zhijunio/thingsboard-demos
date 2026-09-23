package org.example.thingsboard.ruleengine;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;

public record RuleChainToRuleNodeMsg(TbMsg msg, String fromRelationType) implements TbActorMsg {
    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_TO_RULE_MSG;
    }
}
