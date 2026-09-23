package org.example.thingsboard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.io.IOException;
import java.util.List;

/** 把 Bearer JWT 解析成 Spring SecurityContext，供后续授权链使用。 */
public final class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenFactory tokenFactory;

    public JwtAuthenticationFilter(JwtTokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                SecurityUser user = tokenFactory.parseAccessJwtToken(authorization.substring(7));
                if (isFinalAuthority(user.authority())) {
                    SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(user, null,
                                    List.of(new SimpleGrantedAuthority(user.authority().name()))));
                }
            } catch (IllegalArgumentException ignored) {
                // The request remains anonymous and is handled by authorization rules.
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isFinalAuthority(Authority authority) {
        return authority == Authority.SYS_ADMIN
                || authority == Authority.TENANT_ADMIN
                || authority == Authority.CUSTOMER_USER;
    }
}
