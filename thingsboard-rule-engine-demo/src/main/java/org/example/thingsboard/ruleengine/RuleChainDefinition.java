package org.example.thingsboard.ruleengine;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.List;

public record RuleChainDefinition(RuleChainId id, String name, RuleNodeId firstNode,
                                  List<RuleNodeDefinition> nodes, List<RuleNodeRelation> relations) {
    public RuleChainDefinition {
        nodes = List.copyOf(nodes);
        relations = List.copyOf(relations);
    }
}
