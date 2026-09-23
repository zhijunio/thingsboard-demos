package org.example.thingsboard.devicesession;

import org.example.thingsboard.devicesession.DeviceActor.SessionSnapshot;

import java.util.List;

public interface DeviceSessionCacheService {
    List<SessionSnapshot> get(String deviceId);

    void put(String deviceId, List<SessionSnapshot> sessions);
}
