package org.thingsboard.server.service.security.auth;

import org.thingsboard.server.service.security.model.SecurityUser;

public record MfaConfigurationToken(SecurityUser principal) {
}
