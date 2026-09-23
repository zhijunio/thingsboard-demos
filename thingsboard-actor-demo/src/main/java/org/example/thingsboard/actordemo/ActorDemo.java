package org.example.thingsboard.actordemo;

import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbActorSystem;
import org.thingsboard.server.actors.TbActorSystemSettings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ActorDemo {
    private ActorDemo() {
    }

    public static void main(String[] args) throws Exception {
        TbActorSystem actorSystem = new DefaultTbActorSystem(new TbActorSystemSettings(2, 1, 5));
        ExecutorService dispatcher = Executors.newFixedThreadPool(2);
        actorSystem.createDispatcher("demo-dispatcher", dispatcher);

        DemoState state = new DemoState(3);
        DemoState childState = new DemoState(1);
        DeviceState deviceState = new DeviceState();
        DeviceState childDeviceState = new DeviceState();
        TbActorRef root = actorSystem.createRootActor(
                "demo-dispatcher", new DemoActorCreator("demo-root", deviceState, state, 1));
        TbActorRef child = actorSystem.createChildActor(
                "demo-dispatcher",
                new DemoActorCreator("demo-device-1", childDeviceState, childState, 0),
                root.getActorId());

        root.tell(DemoMessage.connect("session-1"));
        root.tell(DemoMessage.telemetry("temperature", 21.5));
        root.tell(DemoMessage.failure());
        root.tellWithHighPriority(DemoMessage.rpcRequest("reboot", "{}"));
        actorSystem.broadcastToChildren(
                root.getActorId(), DemoMessage.telemetry("broadcast", 1));

        if (!state.awaitProcessed(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("actor did not process all successful messages");
        }
        if (!childState.awaitProcessed(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("child actor did not process broadcast message");
        }
        System.out.println("[Actor] processed=" + state.processedTypes());
        System.out.println("[Actor] maxConcurrent=" + state.maxConcurrent());
        System.out.println("[Actor] processFailures=" + state.processFailures());
        System.out.println("[Actor] child=" + child.getActorId() + ", childProcessed=" + childState.processedTypes());
        System.out.println("[Device] session=" + deviceState.sessionId());
        System.out.println("[Device] telemetry=" + deviceState.telemetry());
        System.out.println("[Device] lastRpc=" + deviceState.lastRpcMethod()
                + " " + deviceState.lastRpcParams());

        actorSystem.stop(root);
        state.awaitDestroyed(2, TimeUnit.SECONDS);
        System.out.println("[Actor] destroyReason=" + state.destroyReason());
        actorSystem.stop();
    }
}
