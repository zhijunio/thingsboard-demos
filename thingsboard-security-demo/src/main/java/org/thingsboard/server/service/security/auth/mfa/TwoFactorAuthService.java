package org.thingsboard.server.service.security.auth.mfa;

import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;

public interface TwoFactorAuthService {
    boolean isTwoFaEnabled(String email);

    TotpTwoFaAccountConfig generateNewAccountConfig(String email, String issuer);

    void saveAccountConfig(TotpTwoFaAccountConfig accountConfig);

    boolean checkVerificationCode(String email, String verificationCode);
}
