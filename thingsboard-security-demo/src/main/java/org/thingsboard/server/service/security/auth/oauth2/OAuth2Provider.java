package org.thingsboard.server.service.security.auth.oauth2;

public interface OAuth2Provider {
    OAuth2UserInfo exchangeCode(String code);
}
