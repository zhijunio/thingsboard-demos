package org.thingsboard.server.service.security.auth.oauth2;

import org.thingsboard.server.service.security.model.token.AccessJwtToken;

public record OAuth2LoginResult(AccessJwtToken token, String previousUri) {
}
