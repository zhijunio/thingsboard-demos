package org.thingsboard.server.service.security.auth.rest;

public record LoginResponse(String token, String preVerificationToken, boolean mfaRequired) {
}
