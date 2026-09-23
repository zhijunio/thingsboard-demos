package org.thingsboard.server.service.security.auth.oauth2;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class OAuth2AuthorizationRequestRepository {
    private final Map<String, OAuth2AuthorizationRequest> requests = new ConcurrentHashMap<>();

    public void save(OAuth2AuthorizationRequest request) {
        requests.put(request.state(), request);
    }

    public Optional<OAuth2AuthorizationRequest> consume(String state) {
        return Optional.ofNullable(requests.remove(state));
    }
}
