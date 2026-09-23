package org.thingsboard.server.service.security.auth;

import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.auth.mfa.DefaultTwoFactorAuthService;
import org.thingsboard.server.service.security.auth.rest.LoginResponse;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

public final class SecurityAuthenticationService {
    private final InMemoryUserStore userStore;
    private final PasswordHasher passwordHasher;
    private final JwtTokenFactory tokenFactory;
    private final DefaultTwoFactorAuthService twoFactorAuthService;

    public SecurityAuthenticationService(InMemoryUserStore userStore, PasswordHasher passwordHasher,
                                         JwtTokenFactory tokenFactory,
                                         DefaultTwoFactorAuthService twoFactorAuthService) {
        this.userStore = userStore;
        this.passwordHasher = passwordHasher;
        this.tokenFactory = tokenFactory;
        this.twoFactorAuthService = twoFactorAuthService;
    }

    public LoginResponse login(String username, String password) {
        return issueLoginResponse(authenticatePassword(username, password));
    }

    public SecurityUser authenticatePassword(String username, String password) {
        InMemoryUserStore.UserAccount account = userStore.find(username)
                .orElseThrow(() -> new AuthenticationFailureException("Bad credentials"));
        if (!account.enabled() || !passwordHasher.matches(password, account.passwordHash())) {
            throw new AuthenticationFailureException("Bad credentials");
        }
        return account.securityUser();
    }

    public LoginResponse issueLoginResponse(SecurityUser user) {
        if (twoFactorAuthService.isTwoFaEnabled(user.email())) {
            SecurityUser preVerificationUser = new SecurityUser(user.id(), user.email(), user.tenantId(),
                    Authority.PRE_VERIFICATION_TOKEN, user.enabled());
            return new LoginResponse(null, tokenFactory.createAccessJwtToken(preVerificationUser).token(), true);
        }
        return new LoginResponse(tokenFactory.createAccessJwtToken(user).token(), null, false);
    }

    public AccessJwtToken verifyMfa(String preVerificationToken, String code) {
        SecurityUser preVerificationUser = tokenFactory.parseAccessJwtToken(preVerificationToken);
        if (preVerificationUser.authority() != Authority.PRE_VERIFICATION_TOKEN) {
            throw new AuthenticationFailureException("Token is not a pre-verification token");
        }
        if (!twoFactorAuthService.checkVerificationCode(preVerificationUser.email(), code)) {
            throw new AuthenticationFailureException("Invalid MFA code");
        }
        SecurityUser user = userStore.find(preVerificationUser.email())
                .orElseThrow(() -> new AuthenticationFailureException("User no longer exists"))
                .securityUser();
        return tokenFactory.createAccessJwtToken(user);
    }
}
