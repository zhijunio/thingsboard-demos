package org.example.thingsboard.mqttdemo;

/** 对应源码中的 ValidateDeviceCredentialsResponse，只保留认证是否得到设备信息。 */
public record ValidateDeviceCredentialsResponse(String clientId) {

    public boolean hasDeviceInfo() {
        return clientId != null && !clientId.isBlank();
    }
}
