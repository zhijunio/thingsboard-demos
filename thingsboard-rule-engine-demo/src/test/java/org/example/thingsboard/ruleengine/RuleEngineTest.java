package org.example.thingsboard.ruleengine;

import org.example.thingsboard.ruleengine.nodes.MessageTypeFilterNode;
import org.example.thingsboard.ruleengine.nodes.TelemetryThresholdNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineTest {
    private TbActorSystem actorSystem;
    private ExecutorService dispatcher;
    private TbActorRef ruleChainActor;

    @BeforeEach
    void setUp() {
        actorSystem = new DefaultTbActorSystem(new TbActorSystemSettings(10, 1));
        dispatcher = Executors.newFixedThreadPool(3);
        actorSystem.createDispatcher("rule-dispatcher", dispatcher);

        RuleNodeId filter = new RuleNodeId("filter");
        RuleNodeId threshold = new RuleNodeId("threshold");
        RuleChainDefinition definition = new RuleChainDefinition(
                new RuleChainId("chain"), "test-chain", filter,
                List.of(
                        new RuleNodeDefinition(filter, "filter", new MessageTypeFilterNode("POST_TELEMETRY")),
                        new RuleNodeDefinition(threshold, "threshold", new TelemetryThresholdNode("temperature", 20))),
                List.of(new RuleNodeRelation(filter, threshold, "Success")));
        ruleChainActor = actorSystem.createRootActor("rule-dispatcher", new RuleChainCreator(definition));
    }

    @AfterEach
    void tearDown() {
        actorSystem.stop();
    }

    @Test
    void successRelationRoutesToNextNodeAndCompletesSuccessfully() throws Exception {
        RuleEngineResult callback = new RuleEngineResult();
        ruleChainActor.tell(new QueueToRuleEngineMsg(message("POST_TELEMETRY", 25, callback)));

        RuleEngineResult.Result result = callback.await(2, TimeUnit.SECONDS);
        assertTrue(result.success());
        assertEquals("", result.reason());
    }

    @Test
    void failureRelationWithoutTargetCompletesWithFailure() throws Exception {
        RuleEngineResult callback = new RuleEngineResult();
        ruleChainActor.tell(new QueueToRuleEngineMsg(message("POST_TELEMETRY", 10, callback)));

        RuleEngineResult.Result result = callback.await(2, TimeUnit.SECONDS);
        assertFalse(result.success());
        assertEquals("rule node returned failure", result.reason());
    }

    @Test
    void messageTypeFailureDoesNotReachThresholdNode() throws Exception {
        RuleEngineResult callback = new RuleEngineResult();
        ruleChainActor.tell(new QueueToRuleEngineMsg(message("POST_ATTRIBUTES", 25, callback)));

        RuleEngineResult.Result result = callback.await(2, TimeUnit.SECONDS);
        assertFalse(result.success());
        assertEquals("rule node returned failure", result.reason());
    }

    private TbMsg message(String type, double temperature, RuleEngineResult callback) {
        return TbMsg.newMsg().type(type).originator("device-1")
                .data("temperature", temperature).callback(callback).build();
    }

    private record RuleChainCreator(RuleChainDefinition definition)
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
