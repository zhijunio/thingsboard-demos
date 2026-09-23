package org.thingsboard.server.service.security.auth;

import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserStore {
    private final PasswordHasher passwordHasher;
    private final Map<String, UserAccount> accounts = new ConcurrentHashMap<>();

    public InMemoryUserStore(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    public SecurityUser register(String email, String password, String tenantId, Authority authority) {
        UserAccount account = new UserAccount(UUID.randomUUID(), email, tenantId, authority,
                true, passwordHasher.hash(password));
        if (accounts.putIfAbsent(email, account) != null) {
            throw new IllegalArgumentException("User already exists: " + email);
        }
        return account.securityUser();
    }

    public Optional<UserAccount> find(String email) {
        return Optional.ofNullable(accounts.get(email));
    }

    public record UserAccount(UUID id, String email, String tenantId, Authority authority,
                              boolean enabled, PasswordHasher.PasswordHash passwordHash) {
        public SecurityUser securityUser() {
            return new SecurityUser(id, email, tenantId, authority, enabled);
        }
    }
}
