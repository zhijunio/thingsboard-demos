package org.example.thingsboard.ruleengine;

import org.example.thingsboard.ruleengine.nodes.MessageTypeFilterNode;
import org.example.thingsboard.ruleengine.nodes.TelemetryThresholdNode;
import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbActorSystem;
import org.thingsboard.server.actors.TbActorSystemSettings;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RuleEngineDemo {
    private RuleEngineDemo() {
    }

    public static void main(String[] args) {
        TbActorSystem actorSystem = new DefaultTbActorSystem(new TbActorSystemSettings(10, 1));
        ExecutorService dispatcher = Executors.newFixedThreadPool(4);
        actorSystem.createDispatcher("rule-dispatcher", dispatcher);

        RuleNodeId filterId = new RuleNodeId("message-type-filter");
        RuleNodeId thresholdId = new RuleNodeId("temperature-threshold");
        RuleChainDefinition chain = new RuleChainDefinition(
                new RuleChainId("demo-rule-chain"),
                "Telemetry threshold chain",
                filterId,
                List.of(
                        new RuleNodeDefinition(filterId, "Message Type Filter",
                                new MessageTypeFilterNode("POST_TELEMETRY")),
                        new RuleNodeDefinition(thresholdId, "Temperature >= 20",
                                new TelemetryThresholdNode("temperature", 20))),
                List.of(new RuleNodeRelation(filterId, thresholdId, "Success")));

        TbActorRef ruleChainActor = actorSystem.createRootActor(
                "rule-dispatcher", new RuleChainActorCreator(chain));

        submit(ruleChainActor, telemetry("POST_TELEMETRY", 25));
        submit(ruleChainActor, telemetry("POST_TELEMETRY", 10));
        submit(ruleChainActor, telemetry("POST_ATTRIBUTES", 25));

        System.out.println("规则链已提交 3 条消息，结果通过异步 callback 输出。");
        sleep(500);
        actorSystem.stop();
    }

    private static void submit(TbActorRef chain, TbMsg msg) {
        chain.tell(new QueueToRuleEngineMsg(msg));
    }

    private static TbMsg telemetry(String type, double temperature) {
        RuleEngineResult result = new RuleEngineResult();
        TbMsg msg = TbMsg.newMsg()
                .type(type)
                .originator("device-1")
                .data("temperature", temperature)
                .callback(new PrintingCallback(type, temperature, result))
                .build();
        return msg;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    private record PrintingCallback(String type, double temperature, RuleEngineResult result)
            implements org.thingsboard.server.common.msg.TbMsgCallback {
        @Override
        public void onSuccess() {
            result.onSuccess();
            System.out.println("[RuleEngine] type=" + type + ", temperature=" + temperature + ", result=SUCCESS");
        }

        @Override
        public void onFailure(String reason) {
            result.onFailure(reason);
            System.out.println("[RuleEngine] type=" + type + ", temperature=" + temperature
                    + ", result=FAILURE, reason=" + reason);
        }
    }

    private record RuleChainActorCreator(RuleChainDefinition definition)
            implements org.thingsboard.server.actors.TbActorCreator {
        @Override
        public org.thingsboard.server.actors.TbActorId createActorId() {
            return new org.thingsboard.server.actors.TbStringActorId(definition.id().id());
        }

        @Override
        public org.thingsboard.server.actors.TbActor createActor() {
            return new RuleChainActor(definition);
        }
    }
}
