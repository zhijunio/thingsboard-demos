package org.example.thingsboard.ruleengine;

import org.thingsboard.server.common.data.id.RuleNodeId;

public record RuleNodeRelation(RuleNodeId from, RuleNodeId to, String type) {
}
