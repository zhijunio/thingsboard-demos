package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

/** 对齐 TB 的 TotpTwoFaProvider：生成 otpauth URL，并校验 RFC 6238 TOTP。 */
public final class TotpTwoFaProvider implements TwoFaProvider {
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public TotpTwoFaProvider() {
        this(Clock.systemUTC());
    }

    public TotpTwoFaProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TotpTwoFaAccountConfig generateNewAccountConfig(String email, String issuer) {
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        return new TotpTwoFaAccountConfig(email, issuer, base32Encode(secret));
    }

    @Override
    public boolean checkVerificationCode(String code, TotpTwoFaAccountConfig accountConfig) {
        long timeStep = clock.instant().getEpochSecond() / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            if (codeAt(accountConfig.secret(), (timeStep + offset) * TIME_STEP_SECONDS).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String codeAt(String base32Secret, long epochSeconds) {
        try {
            byte[] secret = base32Decode(base32Secret);
            byte[] counter = ByteBuffer.allocate(Long.BYTES).putLong(epochSeconds / TIME_STEP_SECONDS).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception error) {
            throw new IllegalArgumentException("Unable to calculate TOTP", error);
        }
    }

    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.TOTP;
    }

    private static String base32Encode(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32_ALPHABET.charAt((buffer >>> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            result.append(BASE32_ALPHABET.charAt((buffer << (5 - bits)) & 31));
        }
        return result.toString();
    }

    private static byte[] base32Decode(String value) {
        String normalized = value.replace("=", "").toUpperCase();
        byte[] result = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (char character : normalized.toCharArray()) {
            int digit = BASE32_ALPHABET.indexOf(character);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base32 secret");
            }
            buffer = (buffer << 5) | digit;
            bits += 5;
            if (bits >= 8) {
                result[index++] = (byte) ((buffer >>> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return result;
    }
}
