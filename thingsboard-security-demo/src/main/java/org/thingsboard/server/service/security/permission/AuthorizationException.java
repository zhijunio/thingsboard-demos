package org.thingsboard.server.service.security.permission;

public final class AuthorizationException extends IllegalStateException {
    public AuthorizationException(String message) {
        super(message);
    }
}
