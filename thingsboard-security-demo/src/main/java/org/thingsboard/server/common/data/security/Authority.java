package org.thingsboard.server.common.data.security;

public enum Authority {
    SYS_ADMIN,
    TENANT_ADMIN,
    CUSTOMER_USER,
    REFRESH_TOKEN,
    PRE_VERIFICATION_TOKEN,
    MFA_CONFIGURATION_TOKEN;

    public static Authority parse(String value) {
        for (Authority authority : values()) {
            if (authority.name().equalsIgnoreCase(value)) {
                return authority;
            }
        }
        throw new IllegalArgumentException("Unknown authority: " + value);
    }
}
