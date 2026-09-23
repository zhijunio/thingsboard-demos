package org.example.thingsboard.ruleengine;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.server.actors.AbstractTbActor;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCreator;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbStringActorId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 对应 ThingsBoard RuleChainActor 的核心路由职责。 */
public final class RuleChainActor extends AbstractTbActor {
    private final RuleChainDefinition definition;
    private final Map<RuleNodeId, TbActorRef> nodeActors = new HashMap<>();
    private final Map<RuleNodeId, List<RuleNodeRelation>> nodeRoutes = new HashMap<>();

    public RuleChainActor(RuleChainDefinition definition) {
        this.definition = definition;
        for (RuleNodeDefinition node : definition.nodes()) {
            nodeRoutes.put(node.id(), definition.relations().stream()
                    .filter(relation -> relation.from().equals(node.id()))
                    .toList());
        }
    }

    @Override
    public void init(TbActorCtx ctx) {
        super.init(ctx);
        for (RuleNodeDefinition node : definition.nodes()) {
            TbActorRef actor = ctx.getOrCreateChildActor(new TbStringActorId(node.id().id()),
                    new RuleNodeActorCreator(node, ctx.getActorId()));
            nodeActors.put(node.id(), actor);
        }
    }

    @Override
    public boolean process(TbActorMsg message) {
        if (message.getMsgType() == MsgType.QUEUE_TO_RULE_ENGINE_MSG) {
            pushToFirst(((QueueToRuleEngineMsg) message).msg());
        } else if (message.getMsgType() == MsgType.RULE_TO_RULE_CHAIN_TELL_NEXT_MSG) {
            tellNext((RuleNodeToRuleChainTellNextMsg) message);
        }
        return true;
    }

    private void pushToFirst(TbMsg msg) {
        TbActorRef first = nodeActors.get(definition.firstNode());
        if (first == null) {
            msg.callback().onFailure("first rule node does not exist");
            return;
        }
        first.tell(new RuleChainToRuleNodeMsg(msg, ""));
    }

    private void tellNext(RuleNodeToRuleChainTellNextMsg envelope) {
        List<RuleNodeRelation> matching = nodeRoutes.getOrDefault(envelope.originator(), List.of())
                .stream()
                .filter(relation -> contains(envelope.relationTypes(), relation.type()))
                .toList();
        if (matching.isEmpty()) {
            if (contains(envelope.relationTypes(), "Failure")) {
                envelope.msg().callback().onFailure(envelope.failureMessage());
            } else {
                envelope.msg().callback().onSuccess();
            }
            return;
        }
        for (RuleNodeRelation relation : matching) {
            TbActorRef target = nodeActors.get(relation.to());
            if (target == null) {
                envelope.msg().callback().onFailure("target rule node does not exist: " + relation.to());
            } else {
                envelope.msg().addRoute(definitionName(envelope.originator()), relation.type());
                target.tell(new RuleChainToRuleNodeMsg(envelope.msg(), relation.type()));
            }
        }
    }

    private String definitionName(RuleNodeId id) {
        return definition.nodes().stream().filter(node -> node.id().equals(id))
                .map(RuleNodeDefinition::name).findFirst().orElse(id.id());
    }

    private boolean contains(Set<String> relations, String expected) {
        return relations.stream().anyMatch(value -> value.equalsIgnoreCase(expected));
    }

    private final class RuleNodeActorCreator implements TbActorCreator {
        private final RuleNodeDefinition node;
        private final TbActorId actorId;

        private RuleNodeActorCreator(RuleNodeDefinition node, TbActorId actorId) {
            this.node = node;
            this.actorId = actorId;
        }

        @Override
        public TbActorId createActorId() {
            return new TbStringActorId(node.id().id());
        }

        @Override
        public TbActor createActor() {
            return new RuleNodeActor(definition, node, actorId);
        }
    }
}
