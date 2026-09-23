package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

public interface RuleNodeContext {
    void tellNext(TbMsg msg, Set<String> relationTypes);

    void tellFailure(TbMsg msg, Exception error);
}
