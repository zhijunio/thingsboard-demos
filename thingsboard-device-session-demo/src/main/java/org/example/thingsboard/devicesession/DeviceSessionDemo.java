package org.example.thingsboard.devicesession;

import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorSystem;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class DeviceSessionDemo {
    private DeviceSessionDemo() {
    }

    public static void main(String[] args) throws Exception {
        TbActorSystem actorSystem = new DefaultTbActorSystem();
        DeviceEventSink sink = new DeviceEventSink();
        InMemoryDeviceSessionCache cache = new InMemoryDeviceSessionCache();
        DeviceSessionManager manager = new DeviceSessionManager(
                actorSystem, cache, sink, 200,
                Map.of("device-1", "demo-token"));

        DeviceSessionManager.SessionHandle first = manager.connect("device-1", "demo-token", "node-a");
        manager.subscribeRpc(first);
        manager.telemetry(first, Map.of("temperature", 25.0));
        manager.sendRpc("device-1", new ServerSideRpcRequest(null, "setTemperature", 30));
        waitForEvents(sink, 3);
        System.out.println("[DeviceActor] events=" + sink.events());
        System.out.println("[SessionCache] sessions=" + cache.get("device-1").size());

        manager.activity(first, System.currentTimeMillis() - 1_000);
        manager.checkTimeout("device-1");
        waitForEvents(sink, 4);
        System.out.println("[Timeout] events=" + sink.events());
        System.out.println("[SessionCache] sessions=" + cache.get("device-1").size());
        actorSystem.stop();
    }

    private static void waitForEvents(DeviceEventSink sink, int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (sink.events().size() < count && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }
}
