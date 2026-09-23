package org.thingsboard.server.service.security.model.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 对齐 ThingsBoard JwtTokenFactory 的签发/解析边界，使用同一 JJWT 技术栈。 */
public final class JwtTokenFactory {
    private static final String SCOPES = "scopes";
    private static final String USER_ID = "userId";
    private static final String ENABLED = "enabled";
    private static final String TENANT_ID = "tenantId";
    private final SecretKey secretKey;
    private final Clock clock;
    private final long tokenLifetimeSeconds;

    public JwtTokenFactory(String secret, Clock clock, long tokenLifetimeSeconds) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 64) {
            throw new IllegalArgumentException("HS512 JWT secret must contain at least 64 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.clock = clock;
        this.tokenLifetimeSeconds = tokenLifetimeSeconds;
    }

    public AccessJwtToken createAccessJwtToken(SecurityUser user) {
        return new AccessJwtToken(createToken(user, user.authority(), tokenLifetimeSeconds));
    }

    public AccessJwtToken createMfaToken(SecurityUser user, Authority scope, long expirationSeconds) {
        return new AccessJwtToken(createToken(user, scope, expirationSeconds));
    }

    public AccessJwtToken createRefreshToken(SecurityUser user, long expirationSeconds) {
        return new AccessJwtToken(createToken(user, Authority.REFRESH_TOKEN, expirationSeconds));
    }

    public SecurityUser parseAccessJwtToken(String token) {
        Jws<Claims> signedClaims = parseTokenClaims(token);
        Claims claims = signedClaims.getPayload();
        List<?> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("JWT token does not contain scopes");
        }
        Authority authority = Authority.parse(String.valueOf(scopes.get(0)));
        return new SecurityUser(
                UUID.fromString(claims.get(USER_ID, String.class)),
                claims.getSubject(),
                claims.get(TENANT_ID, String.class),
                authority,
                Boolean.TRUE.equals(claims.get(ENABLED, Boolean.class)));
    }

    private String createToken(SecurityUser user, Authority scope, long expirationSeconds) {
        if (user.authority() == null || user.email() == null || user.email().isBlank()) {
            throw new IllegalArgumentException("JWT user must have email and authority");
        }
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .subject(user.email())
                .claim(SCOPES, List.of(scope.name()))
                .claim(USER_ID, user.id().toString())
                .claim(ENABLED, user.enabled())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)));
        if (user.tenantId() != null) {
            builder.claim(TENANT_ID, user.tenantId());
        }
        return builder.signWith(secretKey, Jwts.SIG.HS512).compact();
    }

    private Jws<Claims> parseTokenClaims(String token) {
        try {
            return Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("JWT token is invalid or expired", error);
        }
    }
}
