package org.thingsboard.rule.engine.api;

public interface RuleNode {
    void onMsg(RuleNodeContext ctx, org.thingsboard.server.common.msg.TbMsg msg) throws Exception;
}
