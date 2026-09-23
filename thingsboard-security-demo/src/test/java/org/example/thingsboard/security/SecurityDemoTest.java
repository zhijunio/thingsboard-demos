package org.example.thingsboard.security;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.service.security.auth.AuthenticationFailureException;
import org.thingsboard.server.service.security.auth.InMemoryUserStore;
import org.thingsboard.server.service.security.auth.PasswordHasher;
import org.thingsboard.server.service.security.auth.SecurityAuthenticationService;
import org.thingsboard.server.service.security.auth.mfa.DefaultTwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.TotpTwoFaProvider;
import org.thingsboard.server.service.security.auth.oauth2.BasicOAuth2ClientMapper;
import org.thingsboard.server.service.security.auth.oauth2.InMemoryOAuth2Provider;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2AuthorizationRequestRepository;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2LoginService;
import org.thingsboard.server.service.security.auth.oauth2.OAuth2UserInfo;
import org.thingsboard.server.service.security.auth.oauth2.Oauth2AuthenticationSuccessHandler;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.AuthorizationException;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityDemoTest {
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);

    @Test
    void localLoginStopsAtPreVerificationTokenUntilTotpSucceeds() {
        PasswordHasher passwordHasher = new PasswordHasher();
        InMemoryUserStore userStore = new InMemoryUserStore(passwordHasher);
        var user = userStore.register("tenant@example.com", "secret", "tenant-1", Authority.TENANT_ADMIN);
        TotpTwoFaProvider provider = new TotpTwoFaProvider(CLOCK);
        DefaultTwoFactorAuthService twoFa = new DefaultTwoFactorAuthService(provider);
        TotpTwoFaAccountConfig account = new TotpTwoFaAccountConfig(user.email(), "TB", "JBSWY3DPEHPK3PXP");
        twoFa.saveAccountConfig(account);
        JwtTokenFactory factory = new JwtTokenFactory("demo-secret-for-thingsboard-security-jwt-0123456789-abcdef-0123456789", CLOCK, 60);
        SecurityAuthenticationService authentication = new SecurityAuthenticationService(
                userStore, passwordHasher, factory, twoFa);

        var login = authentication.login(user.email(), "secret");

        assertTrue(login.mfaRequired());
        assertNotNull(login.preVerificationToken());
        assertThrows(AuthenticationFailureException.class,
                () -> authentication.verifyMfa(login.preVerificationToken(), "000000"));
        String code = provider.codeAt(account.secret(), CLOCK.instant().getEpochSecond());
        assertEquals(Authority.TENANT_ADMIN,
                factory.parseAccessJwtToken(authentication.verifyMfa(login.preVerificationToken(), code).token()).authority());
    }

    @Test
    void totpProviderGeneratesUriAndAcceptsCurrentWindow() {
        TotpTwoFaProvider provider = new TotpTwoFaProvider(CLOCK);
        TotpTwoFaAccountConfig account = new TotpTwoFaAccountConfig("user@example.com", "TB", "JBSWY3DPEHPK3PXP");
        String code = provider.codeAt(account.secret(), CLOCK.instant().getEpochSecond());

        assertTrue(account.authUrl().startsWith("otpauth://totp/TB:user@example.com"));
        assertTrue(provider.checkVerificationCode(code, account));
        assertFalse(provider.checkVerificationCode("000000", account));
    }

    @Test
    void oauth2ValidatesStateExchangesCodeAndIssuesTbToken() {
        JwtTokenFactory factory = new JwtTokenFactory("demo-secret-for-thingsboard-security-jwt-0123456789-abcdef-0123456789", CLOCK, 60);
        InMemoryOAuth2Provider provider = new InMemoryOAuth2Provider();
        provider.authorize("one-time-code", new OAuth2UserInfo(Map.of("email", "oauth@example.com")));
        OAuth2LoginService service = new OAuth2LoginService(new OAuth2AuthorizationRequestRepository(),
                new Oauth2AuthenticationSuccessHandler(new BasicOAuth2ClientMapper("tenant-1"), factory));
        service.register(new OAuth2ClientRegistration("github", "https://github.example/authorize",
                "https://github.example/token", "client-id", "http://localhost/login/oauth2/code/github",
                List.of("openid", "email")), provider);

        String url = service.beginAuthorization("github", "/dashboard");
        String state = url.substring(url.indexOf("state=") + 6).split("&")[0];
        var result = service.handleCallback("github", "one-time-code", state);

        assertEquals("/dashboard", result.previousUri());
        assertEquals("oauth@example.com", factory.parseAccessJwtToken(result.token().token()).email());
        assertThrows(IllegalArgumentException.class,
                () -> service.handleCallback("github", "one-time-code", state));
    }

    @Test
    void tenantBoundaryIsCheckedAfterAuthentication() {
        AccessControlService accessControl = new AccessControlService();
        var tenantAdmin = new org.thingsboard.server.service.security.model.SecurityUser(
                java.util.UUID.randomUUID(), "tenant@example.com", "tenant-1", Authority.TENANT_ADMIN, true);

        accessControl.check(tenantAdmin, Resource.DEVICE, Operation.WRITE, "tenant-1");
        assertThrows(AuthorizationException.class,
                () -> accessControl.check(tenantAdmin, Resource.DEVICE, Operation.WRITE, "tenant-2"));
    }
}
