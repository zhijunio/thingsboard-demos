package org.example.thingsboard.ruleengine.nodes;

import org.example.thingsboard.ruleengine.RuleNodeRelation;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.RuleNodeContext;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

public final class MessageTypeFilterNode implements RuleNode {
    private final String expectedType;

    public MessageTypeFilterNode(String expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public void onMsg(RuleNodeContext ctx, TbMsg msg) {
        ctx.tellNext(msg, Set.of(msg.type().equals(expectedType) ? "Success" : "Failure"));
    }
}
