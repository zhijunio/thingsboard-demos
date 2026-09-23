package org.thingsboard.server.common.data.security.model.oauth2;

import java.util.List;

public record OAuth2ClientRegistration(String registrationId, String authorizationUri,
                                       String tokenUri, String clientId, String redirectUri,
                                       List<String> scopes) {
}
