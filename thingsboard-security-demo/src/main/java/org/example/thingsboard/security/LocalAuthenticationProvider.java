package org.example.thingsboard.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.thingsboard.server.service.security.auth.AuthenticationFailureException;
import org.thingsboard.server.service.security.auth.SecurityAuthenticationService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/** 对齐 TB RestAuthenticationProvider 的本地用户名密码认证边界。 */
public final class LocalAuthenticationProvider implements AuthenticationProvider {
    private final SecurityAuthenticationService authenticationService;

    public LocalAuthenticationProvider(SecurityAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            SecurityUser user = authenticationService.authenticatePassword(
                    authentication.getName(), String.valueOf(authentication.getCredentials()));
            return UsernamePasswordAuthenticationToken.authenticated(user, null,
                    List.of(new SimpleGrantedAuthority(user.authority().name())));
        } catch (AuthenticationFailureException error) {
            throw new BadCredentialsException("Bad credentials", error);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
