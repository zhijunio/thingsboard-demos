package org.example.thingsboard.ruleengine;

import org.thingsboard.rule.engine.api.RuleNodeContext;
import org.thingsboard.server.actors.AbstractTbActor;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

/** 对应 ThingsBoard RuleNodeActor：把 Actor 消息交给节点逻辑处理。 */
public final class RuleNodeActor extends AbstractTbActor {
    private final RuleChainDefinition chain;
    private final RuleNodeDefinition definition;
    private final TbActorId parentId;

    public RuleNodeActor(RuleChainDefinition chain, RuleNodeDefinition definition, TbActorId parentId) {
        this.chain = chain;
        this.definition = definition;
        this.parentId = parentId;
    }

    @Override
    public boolean process(TbActorMsg message) {
        if (message.getMsgType() != MsgType.RULE_CHAIN_TO_RULE_MSG) {
            return false;
        }
        RuleChainToRuleNodeMsg envelope = (RuleChainToRuleNodeMsg) message;
        TbMsg msg = envelope.msg();
        msg.incrementRuleNodeCounter();
        try {
            definition.node().onMsg(new Context(), msg);
        } catch (Exception error) {
            new Context().tellFailure(msg, error);
        }
        return true;
    }

    private final class Context implements RuleNodeContext {
        @Override
        public void tellNext(TbMsg msg, Set<String> relationTypes) {
            ctx.tell(parentId, new RuleNodeToRuleChainTellNextMsg(
                    definition.id(), relationTypes, msg, "rule node returned failure"));
        }

        @Override
        public void tellFailure(TbMsg msg, Exception error) {
            ctx.tell(parentId, new RuleNodeToRuleChainTellNextMsg(
                    definition.id(), Set.of("Failure"), msg, error.getMessage()));
        }
    }
}
