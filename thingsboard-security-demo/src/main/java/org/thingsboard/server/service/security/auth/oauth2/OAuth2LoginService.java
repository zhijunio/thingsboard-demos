package org.thingsboard.server.service.security.auth.oauth2;

import org.thingsboard.server.common.data.security.model.oauth2.OAuth2ClientRegistration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 演示 OAuth2 authorization-code 的 state、callback、code exchange 和本地 token 签发。 */
public final class OAuth2LoginService {
    private final Map<String, OAuth2ClientRegistration> registrations = new ConcurrentHashMap<>();
    private final Map<String, OAuth2Provider> providers = new ConcurrentHashMap<>();
    private final OAuth2AuthorizationRequestRepository requestRepository;
    private final Oauth2AuthenticationSuccessHandler successHandler;
    private final SecureRandom random = new SecureRandom();

    public OAuth2LoginService(OAuth2AuthorizationRequestRepository requestRepository,
                              Oauth2AuthenticationSuccessHandler successHandler) {
        this.requestRepository = requestRepository;
        this.successHandler = successHandler;
    }

    public void register(OAuth2ClientRegistration registration, OAuth2Provider provider) {
        registrations.put(registration.registrationId(), registration);
        providers.put(registration.registrationId(), provider);
    }

    public String beginAuthorization(String registrationId, String previousUri) {
        OAuth2ClientRegistration registration = registration(registrationId);
        String state = randomValue();
        OAuth2AuthorizationRequest request = new OAuth2AuthorizationRequest(registrationId, state,
                randomValue(), registration.redirectUri(), registration.scopes(), previousUri);
        requestRepository.save(request);
        return registration.authorizationUri()
                + "?response_type=code&client_id=" + encode(registration.clientId())
                + "&redirect_uri=" + encode(registration.redirectUri())
                + "&scope=" + encode(String.join(" ", registration.scopes()))
                + "&state=" + encode(state)
                + "&nonce=" + encode(request.nonce());
    }

    public OAuth2LoginResult handleCallback(String registrationId, String code, String state) {
        OAuth2AuthorizationRequest request = requestRepository.consume(state)
                .orElseThrow(() -> new IllegalArgumentException("OAuth2 state is invalid or already used"));
        if (!registrationId.equals(request.registrationId())) {
            throw new IllegalArgumentException("OAuth2 registration does not match state");
        }
        OAuth2Provider provider = providers.get(registrationId);
        if (provider == null) {
            throw new IllegalArgumentException("OAuth2 provider is not configured: " + registrationId);
        }
        return successHandler.onAuthenticationSuccess(provider.exchangeCode(code), request.previousUri());
    }

    private OAuth2ClientRegistration registration(String registrationId) {
        OAuth2ClientRegistration registration = registrations.get(registrationId);
        if (registration == null) {
            throw new IllegalArgumentException("OAuth2 client is not configured: " + registrationId);
        }
        return registration;
    }

    private String randomValue() {
        byte[] value = new byte[24];
        random.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
