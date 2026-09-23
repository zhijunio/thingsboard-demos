package org.example.thingsboard.mqttdemo;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * 真实 Broker 边界演示。
 *
 * 该类只负责模拟设备侧 MQTT CONNECT/PUBLISH；ThingsBoard 服务端的
 * MqttTransportHandler 主链仍由 MqttSourceDemo 和单元测试直接演示。
 */
public final class MqttBrokerDemo {

    private MqttBrokerDemo() {
    }

    public static void main(String[] args) throws Exception {
        String broker = option(args, "--broker", env("TB_MQTT_BROKER", "tcp://localhost:1883"));
        String auth = option(args, "--auth", "token");
        String token = option(args, "--token", env("TB_DEVICE_TOKEN", "demo-token"));
        String userName = option(args, "--username", env("TB_MQTT_USERNAME", "demo-user"));
        String password = option(args, "--password", env("TB_MQTT_PASSWORD", "demo-password"));
        String keyStore = option(args, "--keystore", env("TB_MQTT_KEYSTORE", ""));
        String keyStorePassword = option(args, "--keystore-password",
                env("TB_MQTT_KEYSTORE_PASSWORD", ""));
        String keyPassword = option(args, "--key-password",
                env("TB_MQTT_KEY_PASSWORD", keyStorePassword));
        String keyStoreType = option(args, "--keystore-type", "PKCS12");
        String trustStore = option(args, "--truststore", env("TB_MQTT_TRUSTSTORE", ""));
        String trustStorePassword = option(args, "--truststore-password",
                env("TB_MQTT_TRUSTSTORE_PASSWORD", ""));
        String trustStoreType = option(args, "--truststore-type", "PKCS12");
        int duration = Integer.parseInt(option(args, "--duration", "10"));
        String clientId = "thingsboard-mqtt-demo-" + UUID.randomUUID();

        try (MqttClient client = new MqttClient(broker, clientId)) {
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            if ("basic".equals(auth)) {
                connectOptions.setUserName(userName);
                connectOptions.setPassword(password.toCharArray());
            } else if ("provision".equals(auth)) {
                connectOptions.setUserName("provision");
            } else if ("x509".equals(auth)) {
                if (keyStore.isBlank()) {
                    throw new IllegalArgumentException("x509 auth requires --keystore or TB_MQTT_KEYSTORE");
                }
            } else {
                connectOptions.setUserName(token);
            }
            if (broker.startsWith("ssl://") || broker.startsWith("wss://")) {
                connectOptions.setSocketFactory(createSslContext(
                        keyStore, keyStorePassword, keyPassword, keyStoreType,
                        trustStore, trustStorePassword, trustStoreType).getSocketFactory());
                connectOptions.setHttpsHostnameVerificationEnabled(true);
            } else if (!keyStore.isBlank() || !trustStore.isBlank()) {
                throw new IllegalArgumentException("keystore/truststore require an ssl:// or wss:// broker");
            }
            connectOptions.setCleanSession(true);
            connectOptions.setAutomaticReconnect(false);

            System.out.printf("CONNECT broker=%s clientId=%s auth=%s%n", broker, clientId, auth);
            client.connect(connectOptions);
            System.out.println("CONNACK: connected");

            String rpcTopic = MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + "+";
            IMqttMessageListener rpcListener = (topic, message) -> {
                System.out.printf("RPC request topic=%s payload=%s%n",
                        topic, new String(message.getPayload(), StandardCharsets.UTF_8));
                String requestId = topic.substring(topic.lastIndexOf('/') + 1);
                client.publish(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC + requestId,
                        new MqttMessage("{\"success\":true}".getBytes(StandardCharsets.UTF_8)));
            };
            client.subscribe(rpcTopic, 1, rpcListener);
            publish(client, MqttTopics.DEVICE_ATTRIBUTES_TOPIC, "{\"source\":\"thingsboard-mqtt-demo\"}");
            publish(client, MqttTopics.DEVICE_TELEMETRY_TOPIC, "{\"temperature\":25.3}");
            System.out.printf("PUBLISH telemetry/attributes; waiting %ds for RPC%n", duration);
            Thread.sleep(duration * 1000L);
            client.disconnect();
            System.out.println("DISCONNECT: complete");
        }
    }

    private static void publish(MqttClient client, String topic, String payload) throws Exception {
        client.publish(topic, new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
        System.out.printf("PUBLISH topic=%s payload=%s%n", topic, payload);
    }

    private static String option(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 创建真实 TLS 上下文：keystore 提供客户端证书，truststore 验证 Broker 证书。
     * truststore 未指定时使用 JVM 默认信任库，始终保留证书链和 hostname 校验。
     */
    static SSLContext createSslContext(String keyStorePath,
                                       String keyStorePassword,
                                       String keyPassword,
                                       String keyStoreType,
                                       String trustStorePath,
                                       String trustStorePassword,
                                       String trustStoreType) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        if (keyStorePath == null || keyStorePath.isBlank()) {
            keyManagerFactory.init(null, null);
        } else {
            KeyStore clientKeyStore = loadKeyStore(keyStorePath, keyStorePassword, keyStoreType);
            keyManagerFactory.init(clientKeyStore, keyPassword.toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        if (trustStorePath == null || trustStorePath.isBlank()) {
            trustManagerFactory.init((KeyStore) null);
        } else {
            trustManagerFactory.init(loadKeyStore(trustStorePath, trustStorePassword, trustStoreType));
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom());
        return sslContext;
    }

    private static KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream input = Files.newInputStream(Path.of(path))) {
            keyStore.load(input, password == null ? null : password.toCharArray());
        }
        return keyStore;
    }
}
