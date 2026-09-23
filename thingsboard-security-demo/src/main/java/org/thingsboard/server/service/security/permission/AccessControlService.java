package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 精简 AccessControlService：先验证身份，再验证 authority 和 tenant 边界。 */
public final class AccessControlService {
    public void check(SecurityUser user, Resource resource, Operation operation, String resourceTenantId) {
        if (!user.enabled()) {
            throw new AuthorizationException("User is disabled");
        }
        if (user.authority() == Authority.SYS_ADMIN) {
            return;
        }
        if (user.authority() == Authority.TENANT_ADMIN
                && user.tenantId().equals(resourceTenantId)
                && (operation == Operation.READ || operation == Operation.WRITE || resource == Resource.DEVICE)) {
            return;
        }
        if (user.authority() == Authority.CUSTOMER_USER
                && operation == Operation.READ
                && resource == Resource.DEVICE
                && user.tenantId().equals(resourceTenantId)) {
            return;
        }
        throw new AuthorizationException("Access denied: " + user.authority() + " " + operation + " " + resource);
    }
}
