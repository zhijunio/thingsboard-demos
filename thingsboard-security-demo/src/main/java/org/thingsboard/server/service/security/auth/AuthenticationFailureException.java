package org.thingsboard.server.service.security.auth;

public final class AuthenticationFailureException extends IllegalArgumentException {
    public AuthenticationFailureException(String message) {
        super(message);
    }
}
