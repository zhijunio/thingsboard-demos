package org.example.thingsboard.devicesession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDeviceSessionCache implements DeviceSessionCacheService {
    private final Map<String, List<DeviceActor.SessionSnapshot>> values = new ConcurrentHashMap<>();

    @Override
    public List<DeviceActor.SessionSnapshot> get(String deviceId) {
        return values.getOrDefault(deviceId, List.of());
    }

    @Override
    public void put(String deviceId, List<DeviceActor.SessionSnapshot> sessions) {
        values.put(deviceId, List.copyOf(sessions));
    }
}
