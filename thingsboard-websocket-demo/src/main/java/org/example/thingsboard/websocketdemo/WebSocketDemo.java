package org.example.thingsboard.websocketdemo;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class WebSocketDemo {
    private WebSocketDemo() {
    }

    public static void main(String[] args) throws Exception {
        String accessToken = System.getenv("TB_ACCESS_TOKEN");
        String deviceIdText = System.getenv("TB_DEVICE_ID");
        String endpoint = System.getenv().getOrDefault(
                "TB_WS_ENDPOINT", "ws://localhost:8080/api/ws/plugins/telemetry");
        if (accessToken == null || accessToken.isBlank() || deviceIdText == null || deviceIdText.isBlank()) {
            System.out.println("请设置 TB_ACCESS_TOKEN 和 TB_DEVICE_ID 后运行真实连接示例。");
            System.out.println("endpoint: " + endpoint + "?token=<access-token>");
            System.out.println("订阅命令: " + TelemetryCommandBuilder.latestTelemetry(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"), "temperature", 1));
            return;
        }

        URI uri = URI.create(endpoint + "?token=" + accessToken);
        try (ThingsBoardWebSocketClient client = new ThingsBoardWebSocketClient(uri)) {
            if (!client.connectBlocking(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("ThingsBoard WebSocket 连接失败");
            }
            client.send(TelemetryCommandBuilder.latestTelemetry(
                    UUID.fromString(deviceIdText), "temperature,humidity", 1));
            client.awaitMessage(10, TimeUnit.SECONDS);
        }
    }
}
