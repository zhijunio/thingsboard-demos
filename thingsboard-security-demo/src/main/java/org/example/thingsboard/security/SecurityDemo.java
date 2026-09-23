package org.example.thingsboard.security;

import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.service.security.auth.InMemoryUserStore;
import org.thingsboard.server.service.security.auth.PasswordHasher;
import org.thingsboard.server.service.security.auth.SecurityAuthenticationService;
import org.thingsboard.server.service.security.auth.mfa.DefaultTwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.TotpTwoFaProvider;
import org.thingsboard.server.service.security.auth.oauth2.BasicOAuth2ClientMapper;
import org.thingsboard.server.service.security.auth.oauth2.InMemoryOAuth2Provider;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2AuthorizationRequestRepository;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2LoginService;
import org.thingsboard.server.service.security.auth.oauth2.Oauth2AuthenticationSuccessHandler;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2UserInfo;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public final class SecurityDemo {
    private SecurityDemo() {
    }

    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
        JwtTokenFactory tokenFactory = new JwtTokenFactory(
                "demo-secret-for-thingsboard-security-jwt-0123456789-abcdef-0123456789", clock, 3600);
        PasswordHasher passwordHasher = new PasswordHasher();
        InMemoryUserStore userStore = new InMemoryUserStore(passwordHasher);
        SecurityUser user = userStore.register("tenant@example.com", "tenant-password",
                "tenant-1", Authority.TENANT_ADMIN);

        TotpTwoFaProvider totpProvider = new TotpTwoFaProvider(clock);
        DefaultTwoFactorAuthService twoFa = new DefaultTwoFactorAuthService(totpProvider);
        TotpTwoFaAccountConfig account = new TotpTwoFaAccountConfig(user.email(), "ThingsBoard Demo",
                "JBSWY3DPEHPK3PXP");
        twoFa.saveAccountConfig(account);
        SecurityAuthenticationService authentication = new SecurityAuthenticationService(
                userStore, passwordHasher, tokenFactory, twoFa);

        var login = authentication.login(user.email(), "tenant-password");
        String mfaCode = totpProvider.codeAt(account.secret(), clock.instant().getEpochSecond());
        String accessToken = authentication.verifyMfa(login.preVerificationToken(), mfaCode).token();
        SecurityUser authenticatedUser = tokenFactory.parseAccessJwtToken(accessToken);
        System.out.println("[Local + MFA] authority=" + authenticatedUser.authority()
                + ", mfaRequired=" + login.mfaRequired());

        InMemoryOAuth2Provider provider = new InMemoryOAuth2Provider();
        provider.authorize("demo-code", new OAuth2UserInfo(Map.of("email", "oauth@example.com")));
        OAuth2LoginService oauth2 = new OAuth2LoginService(
                new OAuth2AuthorizationRequestRepository(),
                new Oauth2AuthenticationSuccessHandler(new BasicOAuth2ClientMapper("tenant-1"), tokenFactory));
        oauth2.register(new OAuth2ClientRegistration("demo-provider", "https://idp.example/authorize",
                "https://idp.example/token", "demo-client", "http://localhost/login/oauth2/code/demo-provider",
                List.of("openid", "email", "profile")), provider);
        String authorizationUrl = oauth2.beginAuthorization("demo-provider", "/devices");
        String state = authorizationUrl.substring(authorizationUrl.indexOf("state=") + 6).split("&")[0];
        var oauthResult = oauth2.handleCallback("demo-provider", "demo-code", state);
        System.out.println("[OAuth2] previousUri=" + oauthResult.previousUri()
                + ", tokenSubject=" + tokenFactory.parseAccessJwtToken(oauthResult.token().token()).email());

        new AccessControlService().check(authenticatedUser, Resource.DEVICE, Operation.WRITE, "tenant-1");
        System.out.println("[Authorization] TENANT_ADMIN can WRITE DEVICE in tenant-1");
    }
}
