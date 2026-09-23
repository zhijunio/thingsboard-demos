package org.example.thingsboard.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.service.security.auth.InMemoryUserStore;
import org.thingsboard.server.service.security.auth.PasswordHasher;
import org.thingsboard.server.service.security.auth.SecurityAuthenticationService;
import org.thingsboard.server.service.security.auth.mfa.DefaultTwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.TotpTwoFaProvider;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityDemoConfiguration {
    @Bean
    Clock securityClock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }

    @Bean
    InMemoryUserStore userStore(PasswordHasher passwordHasher) {
        InMemoryUserStore store = new InMemoryUserStore(passwordHasher);
        store.register("tenant@example.com", "tenant-password", "tenant-1", Authority.TENANT_ADMIN);
        return store;
    }

    @Bean
    JwtTokenFactory jwtTokenFactory(Clock securityClock) {
        String secret = System.getenv().getOrDefault("TB_DEMO_JWT_SECRET",
                "demo-secret-for-thingsboard-security-jwt-0123456789-abcdef-0123456789");
        return new JwtTokenFactory(secret, securityClock, 3600);
    }

    @Bean
    TotpTwoFaProvider totpTwoFaProvider(Clock securityClock) {
        return new TotpTwoFaProvider(securityClock);
    }

    @Bean
    DefaultTwoFactorAuthService twoFactorAuthService(TotpTwoFaProvider provider, InMemoryUserStore userStore) {
        DefaultTwoFactorAuthService service = new DefaultTwoFactorAuthService(provider);
        service.saveAccountConfig(new TotpTwoFaAccountConfig(
                userStore.find("tenant@example.com").orElseThrow().email(),
                "ThingsBoard Demo", "JBSWY3DPEHPK3PXP"));
        return service;
    }

    @Bean
    SecurityAuthenticationService securityAuthenticationService(InMemoryUserStore userStore,
                                                                PasswordHasher passwordHasher,
                                                                JwtTokenFactory tokenFactory,
                                                                DefaultTwoFactorAuthService twoFa) {
        return new SecurityAuthenticationService(userStore, passwordHasher, tokenFactory, twoFa);
    }

    @Bean
    LocalAuthenticationProvider localAuthenticationProvider(SecurityAuthenticationService service) {
        return new LocalAuthenticationProvider(service);
    }

    @Bean
    AuthenticationManager authenticationManager(LocalAuthenticationProvider provider) {
        return new ProviderManager(List.of(provider));
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenFactory tokenFactory) {
        return new JwtAuthenticationFilter(tokenFactory);
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("demo-provider")
                .clientId("demo-client")
                .clientSecret("demo-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri("https://idp.example/authorize")
                .tokenUri("https://idp.example/token")
                .userInfoUri("https://idp.example/userinfo")
                .userNameAttributeName("email")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    SpringOAuth2AuthenticationSuccessHandler oauth2SuccessHandler(JwtTokenFactory tokenFactory) {
        return new SpringOAuth2AuthenticationSuccessHandler(tokenFactory);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtFilter,
                                            SpringOAuth2AuthenticationSuccessHandler oauth2SuccessHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI().startsWith("/api/")))
                .oauth2Login(oauth2 -> oauth2.successHandler(oauth2SuccessHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
