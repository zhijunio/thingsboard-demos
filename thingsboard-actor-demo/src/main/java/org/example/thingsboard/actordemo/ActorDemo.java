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
        TbActorRef root = actorSystem.createRootActor(
                "demo-dispatcher", new DemoActorCreator("demo-root", state, 1));
        TbActorRef child = actorSystem.createChildActor(
                "demo-dispatcher", new DemoActorCreator("demo-device-1", childState, 0), root.getActorId());

        root.tell(new DemoMessage(1));
        root.tell(new DemoMessage(2));
        root.tell(new DemoMessage(3, true, null));
        root.tellWithHighPriority(new DemoMessage(99));
        actorSystem.broadcastToChildren(root.getActorId(), new DemoMessage(7));

        if (!state.awaitProcessed(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("actor did not process all successful messages");
        }
        if (!childState.awaitProcessed(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("child actor did not process broadcast message");
        }
        System.out.println("[Actor] processed=" + state.processedValues());
        System.out.println("[Actor] maxConcurrent=" + state.maxConcurrent());
        System.out.println("[Actor] processFailures=" + state.processFailures());
        System.out.println("[Actor] child=" + child.getActorId() + ", childProcessed=" + childState.processedValues());

        actorSystem.stop(root);
        state.awaitDestroyed(2, TimeUnit.SECONDS);
        System.out.println("[Actor] destroyReason=" + state.destroyReason());
        actorSystem.stop();
    }
}
