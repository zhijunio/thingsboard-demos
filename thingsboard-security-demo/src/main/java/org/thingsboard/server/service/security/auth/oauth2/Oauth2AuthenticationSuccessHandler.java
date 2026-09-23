package org.thingsboard.server.service.security.auth.oauth2;

import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

/** 对齐 TB OAuth2 success handler：映射外部主体，再签发 ThingsBoard JWT。 */
public final class Oauth2AuthenticationSuccessHandler {
    private final OAuth2ClientMapper clientMapper;
    private final JwtTokenFactory tokenFactory;

    public Oauth2AuthenticationSuccessHandler(OAuth2ClientMapper clientMapper, JwtTokenFactory tokenFactory) {
        this.clientMapper = clientMapper;
        this.tokenFactory = tokenFactory;
    }

    public OAuth2LoginResult onAuthenticationSuccess(OAuth2UserInfo userInfo, String previousUri) {
        SecurityUser securityUser = clientMapper.map(userInfo);
        AccessJwtToken token = tokenFactory.createAccessJwtToken(securityUser);
        return new OAuth2LoginResult(token, previousUri);
    }
}
