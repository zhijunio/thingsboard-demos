package org.example.thingsboard.actordemo;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbActorSystem;
import org.thingsboard.server.actors.TbActorSystemSettings;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActorSystemTest {

    @Test
    void actorRetriesInitializationAndProcessesMessagesSerially() throws Exception {
        TbActorSystem system = new DefaultTbActorSystem(new TbActorSystemSettings(2, 1, 5));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        system.createDispatcher("test", executor);
        try {
            DemoState state = new DemoState(2);
            TbActorRef actor = system.createRootActor("test", new DemoActorCreator("root", state, 1));
            actor.tell(new DemoMessage(1));
            actor.tell(new DemoMessage(2, true, null));
            actor.tell(new DemoMessage(3));

            assertTrue(state.awaitProcessed(2, TimeUnit.SECONDS));
            assertEquals(1, state.maxConcurrent());
            assertEquals(1, state.processFailures());
            assertEquals(2, state.processedValues().size());
        } finally {
            system.stop();
        }
    }

    @Test
    void stoppedActorNotifiesQueuedMessages() throws Exception {
        TbActorSystem system = new DefaultTbActorSystem(new TbActorSystemSettings(1, 1, 1));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        system.createDispatcher("test", executor);
        try {
            DemoState state = new DemoState(0);
            TbActorRef actor = system.createRootActor("test", new DemoActorCreator("root", state, 0));
            AtomicReference<TbActorStopReason> stopped = new AtomicReference<>();
            actor.tell(new DemoMessage(1, false, stopped));
            actor.tell(new DemoMessage(2, false, stopped));
            system.stop(actor);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (stopped.get() == null && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(TbActorStopReason.STOPPED, stopped.get());
        } finally {
            system.stop();
        }
    }
}
