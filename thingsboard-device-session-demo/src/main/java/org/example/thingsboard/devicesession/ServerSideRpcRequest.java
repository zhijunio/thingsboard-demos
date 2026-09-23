package org.example.thingsboard.devicesession;

import java.util.UUID;

public record ServerSideRpcRequest(UUID id, String method, Object params) {
}
