package org.thingsboard.server.service.security.model;

import org.thingsboard.server.common.data.security.Authority;

import java.util.UUID;

/** ThingsBoard SecurityUser 的精简模型，保留身份、租户和权限上下文。 */
public record SecurityUser(UUID id, String email, String tenantId,
                           Authority authority, boolean enabled) {
    public boolean isSystemAdmin() {
        return authority == Authority.SYS_ADMIN;
    }

    public boolean isTenantAdmin() {
        return authority == Authority.TENANT_ADMIN;
    }
}
