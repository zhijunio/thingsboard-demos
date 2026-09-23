package org.thingsboard.server.service.security.auth.oauth2;

import java.util.List;

public record OAuth2AuthorizationRequest(String registrationId, String state, String nonce,
                                         String redirectUri, List<String> scopes, String previousUri) {
}
