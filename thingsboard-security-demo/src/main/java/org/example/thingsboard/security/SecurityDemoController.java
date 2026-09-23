package org.example.thingsboard.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.service.security.auth.AuthenticationFailureException;
import org.thingsboard.server.service.security.auth.SecurityAuthenticationService;
import org.thingsboard.server.service.security.auth.rest.LoginResponse;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.AuthorizationException;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SecurityDemoController {
    private final AuthenticationManager authenticationManager;
    private final SecurityAuthenticationService authenticationService;
    private final AccessControlService accessControlService = new AccessControlService();

    public SecurityDemoController(AuthenticationManager authenticationManager,
                                  SecurityAuthenticationService authenticationService) {
        this.authenticationManager = authenticationManager;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return authenticationService.issueLoginResponse((SecurityUser) authentication.getPrincipal());
    }

    @PostMapping("/auth/mfa")
    public LoginResponse verifyMfa(@RequestBody MfaRequest request) {
        return new LoginResponse(authenticationService.verifyMfa(request.preVerificationToken(), request.code()).token(),
                null, false);
    }

    @GetMapping("/devices/{tenantId}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public Map<String, String> readDevices(@PathVariable("tenantId") String tenantId, Authentication authentication) {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        accessControlService.check(user, Resource.DEVICE, Operation.READ, tenantId);
        return Map.of("tenantId", tenantId, "result", "authorized");
    }

    @ExceptionHandler({AuthenticationException.class, AuthenticationFailureException.class})
    ResponseEntity<Map<String, String>> authenticationFailure(Exception error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(AuthorizationException.class)
    ResponseEntity<Map<String, String>> authorizationFailure(AuthorizationException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", error.getMessage()));
    }

    public record LoginRequest(String username, String password) {
    }

    public record MfaRequest(String preVerificationToken, String code) {
    }
}
