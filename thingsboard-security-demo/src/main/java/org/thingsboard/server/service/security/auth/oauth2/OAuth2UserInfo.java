package org.thingsboard.server.service.security.auth.oauth2;

import java.util.Map;

public record OAuth2UserInfo(Map<String, Object> attributes) {
    public String email() {
        Object email = attributes.get("email");
        if (!(email instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("OAuth2 provider did not return an email");
        }
        return value;
    }
}
