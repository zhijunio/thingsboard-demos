package org.thingsboard.server.service.security.auth.oauth2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 模拟外部 OAuth2/OIDC provider 的 authorization code -> userinfo 交换。 */
public final class InMemoryOAuth2Provider implements OAuth2Provider {
    private final Map<String, OAuth2UserInfo> authorizationCodes = new ConcurrentHashMap<>();

    public void authorize(String code, OAuth2UserInfo userInfo) {
        authorizationCodes.put(code, userInfo);
    }

    @Override
    public OAuth2UserInfo exchangeCode(String code) {
        OAuth2UserInfo userInfo = authorizationCodes.remove(code);
        if (userInfo == null) {
            throw new IllegalArgumentException("OAuth2 authorization code is invalid or already used");
        }
        return userInfo;
    }
}
