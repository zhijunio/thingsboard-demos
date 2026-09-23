package org.thingsboard.server.service.security.auth.oauth2;

import org.thingsboard.server.service.security.model.SecurityUser;

public interface OAuth2ClientMapper {
    SecurityUser map(OAuth2UserInfo userInfo);
}
