package org.thingsboard.server.service.security.auth.mfa.provider;

import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

public interface TwoFaProvider {
    TotpTwoFaAccountConfig generateNewAccountConfig(String email, String issuer);

    boolean checkVerificationCode(String code, TotpTwoFaAccountConfig accountConfig);

    TwoFaProviderType getType();
}
