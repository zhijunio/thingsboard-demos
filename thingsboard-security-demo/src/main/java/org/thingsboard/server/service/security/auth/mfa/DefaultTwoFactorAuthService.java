package org.thingsboard.server.service.security.auth.mfa;

import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.TotpTwoFaProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 保留 TB 服务层的 provider 注册和账号配置分离。 */
public final class DefaultTwoFactorAuthService implements TwoFactorAuthService {
    private final TotpTwoFaProvider provider;
    private final Map<String, TotpTwoFaAccountConfig> accounts = new ConcurrentHashMap<>();

    public DefaultTwoFactorAuthService(TotpTwoFaProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean isTwoFaEnabled(String email) {
        return accounts.containsKey(email);
    }

    @Override
    public TotpTwoFaAccountConfig generateNewAccountConfig(String email, String issuer) {
        return provider.generateNewAccountConfig(email, issuer);
    }

    @Override
    public void saveAccountConfig(TotpTwoFaAccountConfig accountConfig) {
        accounts.put(accountConfig.email(), accountConfig);
    }

    @Override
    public boolean checkVerificationCode(String email, String verificationCode) {
        TotpTwoFaAccountConfig accountConfig = accounts.get(email);
        return accountConfig != null && provider.checkVerificationCode(verificationCode, accountConfig);
    }
}
