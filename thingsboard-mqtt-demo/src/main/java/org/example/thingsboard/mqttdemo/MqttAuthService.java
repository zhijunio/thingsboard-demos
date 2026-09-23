package org.example.thingsboard.mqttdemo;

/**
 * 对应源码中由 transport service 完成的 MQTT 凭据校验。
 *
 * 示例保留 ThingsBoard MQTT CONNECT 的 basic credentials 和 X.509 两类校验，
 * 省略租户、设备和凭据数据库。
 */
public interface MqttAuthService {

    boolean validateBasic(String clientId, String userName, String password);

    boolean validateX509(String certificateHash);
}
