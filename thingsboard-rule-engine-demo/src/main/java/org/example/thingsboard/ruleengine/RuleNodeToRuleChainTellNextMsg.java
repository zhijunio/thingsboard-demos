package org.example.thingsboard.ruleengine;

import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

public record RuleNodeToRuleChainTellNextMsg(RuleNodeId originator, Set<String> relationTypes,
                                              TbMsg msg, String failureMessage) implements TbActorMsg {
    public RuleNodeToRuleChainTellNextMsg {
        relationTypes = Set.copyOf(relationTypes);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_TO_RULE_CHAIN_TELL_NEXT_MSG;
    }
}
