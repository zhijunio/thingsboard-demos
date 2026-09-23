package org.thingsboard.server.service.security.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private final SecureRandom random = new SecureRandom();

    public PasswordHash hash(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return new PasswordHash(salt, derive(password, salt, ITERATIONS));
    }

    public boolean matches(String password, PasswordHash expected) {
        return Arrays.equals(expected.digest(), derive(password, expected.salt(), expected.iterations()));
    }

    public record PasswordHash(byte[] salt, byte[] digest, int iterations) {
        private PasswordHash(byte[] salt, byte[] digest) {
            this(salt, digest, ITERATIONS);
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to hash password", error);
        }
    }
}
