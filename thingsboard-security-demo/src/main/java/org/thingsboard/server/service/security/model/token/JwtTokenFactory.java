package org.thingsboard.server.service.security.model.token;

import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 对齐 JwtTokenFactory 的签发/解析边界，使用 JDK 实现 HS512 以减少依赖。 */
public final class JwtTokenFactory {
    private static final String HEADER = "{\"alg\":\"HS512\",\"typ\":\"JWT\"}";
    private static final Pattern STRING_CLAIM = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern NUMBER_CLAIM = Pattern.compile("\\\"%s\\\"\\s*:\\s*(\\d+)");
    private final byte[] secret;
    private final Clock clock;
    private final long tokenLifetimeSeconds;

    public JwtTokenFactory(String secret, Clock clock, long tokenLifetimeSeconds) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
        this.tokenLifetimeSeconds = tokenLifetimeSeconds;
    }

    public AccessJwtToken createAccessJwtToken(SecurityUser user) {
        long expiration = Instant.now(clock).getEpochSecond() + tokenLifetimeSeconds;
        String payload = "{"
                + "\"sub\":\"" + escape(user.email()) + "\","
                + "\"userId\":\"" + user.id() + "\","
                + "\"tenantId\":\"" + escape(user.tenantId()) + "\","
                + "\"authority\":\"" + user.authority() + "\","
                + "\"enabled\":" + user.enabled() + ","
                + "\"exp\":" + expiration
                + "}";
        String encodedHeader = encode(HEADER.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        String unsigned = encodedHeader + "." + encodedPayload;
        return new AccessJwtToken(unsigned + "." + encode(sign(unsigned)));
    }

    public SecurityUser parseAccessJwtToken(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT token is malformed");
        }
        byte[] expected = sign(parts[0] + "." + parts[1]);
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("JWT signature is malformed", error);
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("JWT signature is invalid");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        long expiration = numberClaim(payload, "exp");
        if (expiration <= Instant.now(clock).getEpochSecond()) {
            throw new IllegalArgumentException("JWT token is expired");
        }
        return new SecurityUser(
                UUID.fromString(stringClaim(payload, "userId")),
                stringClaim(payload, "sub"),
                stringClaim(payload, "tenantId"),
                Authority.parse(stringClaim(payload, "authority")),
                Boolean.parseBoolean(booleanClaim(payload, "enabled")));
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret, "HmacSHA512"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw new IllegalStateException("Unable to sign JWT", error);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stringClaim(String payload, String name) {
        Matcher matcher = Pattern.compile(String.format(STRING_CLAIM.pattern(), name)).matcher(payload);
        if (!matcher.find()) {
            throw new IllegalArgumentException("JWT claim is missing: " + name);
        }
        return matcher.group(1);
    }

    private static String booleanClaim(String payload, String name) {
        String marker = "\"" + name + "\":";
        int start = payload.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("JWT claim is missing: " + name);
        }
        int valueStart = start + marker.length();
        int valueEnd = payload.indexOf(',', valueStart);
        if (valueEnd < 0) {
            valueEnd = payload.indexOf('}', valueStart);
        }
        return payload.substring(valueStart, valueEnd);
    }

    private static long numberClaim(String payload, String name) {
        Matcher matcher = Pattern.compile(String.format(NUMBER_CLAIM.pattern(), name)).matcher(payload);
        if (!matcher.find()) {
            throw new IllegalArgumentException("JWT claim is missing: " + name);
        }
        return Long.parseLong(matcher.group(1));
    }
}
