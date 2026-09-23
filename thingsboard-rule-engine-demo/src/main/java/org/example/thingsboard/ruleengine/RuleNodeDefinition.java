package org.example.thingsboard.ruleengine;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.server.common.data.id.RuleNodeId;

public record RuleNodeDefinition(RuleNodeId id, String name, RuleNode node) {
}
