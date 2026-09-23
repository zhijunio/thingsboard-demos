package org.example.thingsboard.mqttdemo;

import java.util.Objects;

/** 用固定 token 模拟 ThingsBoard 的设备凭据校验结果。 */
public final class InMemoryMqttAuthService implements MqttAuthService {

    private final String accessToken;
    private final String userName;
    private final String password;
    private final String certificateHash;

    public InMemoryMqttAuthService(String accessToken) {
        this(accessToken, "demo-user", "demo-password", "demo-certificate-sha3");
    }

    public InMemoryMqttAuthService(String accessToken,
                                   String userName,
                                   String password,
                                   String certificateHash) {
        this.accessToken = Objects.requireNonNull(accessToken);
        this.userName = Objects.requireNonNull(userName);
        this.password = Objects.requireNonNull(password);
        this.certificateHash = Objects.requireNonNull(certificateHash);
    }

    @Override
    public boolean validateBasic(String clientId, String userName, String password) {
        return clientId != null && !clientId.isBlank()
                && (password == null && accessToken.equals(userName)
                || password != null && this.userName.equals(userName) && this.password.equals(password));
    }

    @Override
    public boolean validateX509(String certificateHash) {
        return this.certificateHash.equals(certificateHash);
    }
}
