package org.thingsboard.server.common.data.security.model.mfa.account;

public record TotpTwoFaAccountConfig(String email, String issuer, String secret) {
    public String authUrl() {
        return "otpauth://totp/" + issuer + ":" + email
                + "?issuer=" + issuer + "&secret=" + secret;
    }
}
