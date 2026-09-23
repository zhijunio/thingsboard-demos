package org.thingsboard.server.service.security.auth.oauth2;

import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

/** 对齐 BasicOAuth2ClientMapper：从 provider attributes 读取 email，再映射本地用户。 */
public final class BasicOAuth2ClientMapper implements OAuth2ClientMapper {
    private final String tenantId;

    public BasicOAuth2ClientMapper(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public SecurityUser map(OAuth2UserInfo userInfo) {
        return new SecurityUser(UUID.nameUUIDFromBytes(userInfo.email().getBytes()),
                userInfo.email(), tenantId, Authority.TENANT_ADMIN, true);
    }
}
