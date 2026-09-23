package org.example.thingsboard.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.thingsboard.server.service.security.auth.oauth2.BasicOAuth2ClientMapper;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2ClientMapper;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2UserInfo;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Spring OAuth2 登录成功后，复用 TB 风格 mapper 并换发平台 JWT。 */
public final class SpringOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuth2ClientMapper clientMapper;
    private final JwtTokenFactory tokenFactory;

    public SpringOAuth2AuthenticationSuccessHandler(JwtTokenFactory tokenFactory) {
        this.clientMapper = new BasicOAuth2ClientMapper("tenant-1");
        this.tokenFactory = tokenFactory;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauth2Authentication.getPrincipal();
        var user = clientMapper.map(new OAuth2UserInfo(principal.getAttributes()));
        String token = tokenFactory.createAccessJwtToken(user).token();
        response.sendRedirect("/?accessToken=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
    }
}
