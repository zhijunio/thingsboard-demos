package org.example.thingsboard.ruleengine.nodes;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.RuleNodeContext;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

public final class TelemetryThresholdNode implements RuleNode {
    private final String key;
    private final double threshold;

    public TelemetryThresholdNode(String key, double threshold) {
        this.key = key;
        this.threshold = threshold;
    }

    @Override
    public void onMsg(RuleNodeContext ctx, TbMsg msg) {
        Object value = msg.data().get(key);
        if (!(value instanceof Number number)) {
            ctx.tellNext(msg, Set.of("Failure"));
            return;
        }
        ctx.tellNext(msg, Set.of(number.doubleValue() >= threshold ? "Success" : "Failure"));
    }
}
