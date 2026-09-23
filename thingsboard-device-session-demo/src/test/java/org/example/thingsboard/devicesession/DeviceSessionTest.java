package org.example.thingsboard.devicesession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorSystem;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceSessionTest {
    private TbActorSystem actorSystem;
    private DeviceEventSink sink;
    private InMemoryDeviceSessionCache cache;
    private DeviceSessionManager manager;

    @BeforeEach
    void setUp() {
        actorSystem = new DefaultTbActorSystem();
        sink = new DeviceEventSink();
        cache = new InMemoryDeviceSessionCache();
        manager = new DeviceSessionManager(actorSystem, cache, sink, 100,
                Map.of("device-1", "demo-token"));
    }

    @AfterEach
    void tearDown() {
        actorSystem.stop();
    }

    @Test
    void invalidCredentialsAreRejectedBeforeActorCreation() {
        assertThrows(SecurityException.class, () -> manager.connect("device-1", "wrong", "node-a"));
        assertEquals(0, sink.events().size());
    }

    @Test
    void multipleSessionsShareOneDeviceActorAndRpcGoesToSubscribedSession() throws Exception {
        DeviceSessionManager.SessionHandle first = manager.connect("device-1", "demo-token", "node-a");
        DeviceSessionManager.SessionHandle second = manager.connect("device-1", "demo-token", "node-b");
        manager.subscribeRpc(first);
        manager.sendRpc("device-1", new ServerSideRpcRequest(null, "reboot", true));
        awaitEvents(2);

        assertEquals(1, sink.events().stream().filter(event -> event.startsWith("CONNECTED")).count());
        assertEquals(1, sink.events().stream().filter(event -> event.startsWith("RPC")).count());
        assertEquals(2, cache.get("device-1").size());

        manager.close(first);
        awaitEvents(3);
        assertEquals(0, sink.events().stream().filter(event -> event.startsWith("DISCONNECTED")).count());
        manager.close(second);
        awaitEvents(4);
        assertEquals(1, sink.events().stream().filter(event -> event.startsWith("DISCONNECTED")).count());
    }

    @Test
    void timeoutRemovesInactiveSessionAndUpdatesCache() throws Exception {
        DeviceSessionManager.SessionHandle session = manager.connect("device-1", "demo-token", "node-a");
        manager.activity(session, System.currentTimeMillis() - 1_000);
        manager.checkTimeout("device-1");
        awaitEvents(2);

        assertTrue(sink.events().stream().anyMatch(event -> event.contains("SESSION_TIMEOUT")));
        assertEquals(0, cache.get("device-1").size());
    }

    private void awaitEvents(int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (sink.events().size() < count && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }
}
