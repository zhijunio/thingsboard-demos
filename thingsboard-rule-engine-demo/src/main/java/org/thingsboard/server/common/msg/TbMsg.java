package org.thingsboard.server.common.msg;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class TbMsg {
    private final UUID id;
    private final String type;
    private final String originator;
    private final Map<String, Object> data;
    private final Map<String, String> metadata;
    private final TbMsgCallback callback;
    private final List<String> route;
    private final AtomicInteger ruleNodeCounter;
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;

    private TbMsg(UUID id, String type, String originator, Map<String, Object> data,
                  Map<String, String> metadata, TbMsgCallback callback, List<String> route,
                  AtomicInteger ruleNodeCounter, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        this.id = id;
        this.type = type;
        this.originator = originator;
        this.data = Map.copyOf(data);
        this.metadata = Map.copyOf(metadata);
        this.callback = callback;
        this.route = route;
        this.ruleNodeCounter = ruleNodeCounter;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
    }

    public static Builder newMsg() {
        return new Builder();
    }

    public TbMsg copy() {
        return new TbMsg(id, type, originator, data, metadata, callback,
                new ArrayList<>(route), ruleNodeCounter, ruleChainId, ruleNodeId);
    }

    public UUID id() {
        return id;
    }

    public String type() {
        return type;
    }

    public String originator() {
        return originator;
    }

    public Map<String, Object> data() {
        return data;
    }

    public TbMsgCallback callback() {
        return callback;
    }

    public List<String> route() {
        return List.copyOf(route);
    }

    public int incrementRuleNodeCounter() {
        return ruleNodeCounter.incrementAndGet();
    }

    public TbMsg addRoute(String nodeName, String relationType) {
        route.add(nodeName + " --" + relationType + "--> ");
        return this;
    }

    public RuleChainId ruleChainId() {
        return ruleChainId;
    }

    public RuleNodeId ruleNodeId() {
        return ruleNodeId;
    }

    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private String type;
        private String originator;
        private Map<String, Object> data = new LinkedHashMap<>();
        private Map<String, String> metadata = new LinkedHashMap<>();
        private TbMsgCallback callback;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder originator(String originator) {
            this.originator = originator;
            return this;
        }

        public Builder data(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder callback(TbMsgCallback callback) {
            this.callback = callback;
            return this;
        }

        public TbMsg build() {
            return new TbMsg(Objects.requireNonNull(id), Objects.requireNonNull(type),
                    Objects.requireNonNull(originator), data, metadata, Objects.requireNonNull(callback),
                    new ArrayList<>(), new AtomicInteger(), null, null);
        }
    }
}
