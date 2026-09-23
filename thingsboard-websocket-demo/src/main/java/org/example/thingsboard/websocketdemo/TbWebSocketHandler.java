package org.example.thingsboard.websocketdemo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThingsBoard TbWebSocketHandler 的精简服务端实现。
 *
 * 保留源码的职责边界：连接建立时创建 session、从 telemetry URL 读取 token、
 * 文本消息解析、命令分发、关闭时移除 session。
 */
@Component
public final class TbWebSocketHandler extends TextWebSocketHandler implements WebSocketHandler {
    private final Map<String, SessionMetadata> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        if (!path.startsWith(WebSocketConfiguration.WS_PLUGINS_ENDPOINT + "telemetry")) {
            session.close(CloseStatus.BAD_DATA.withReason("telemetry endpoint is required"));
            return;
        }
        String token = queryParam(session.getUri(), "token");
        if (!"demo-token".equals(token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("invalid token"));
            return;
        }
        sessions.put(session.getId(), new SessionMetadata(UUID.randomUUID().toString(), token));
        System.out.println("[Spring WebSocket] session established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (!sessions.containsKey(session.getId())) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("session is not authenticated"));
            return;
        }
        try {
            JsonObject wrapper = JsonParser.parseString(message.getPayload()).getAsJsonObject();
            handleTelemetryCommands(session, wrapper.getAsJsonArray("tsSubCmds"));
            handleAttributeCommands(session, wrapper.getAsJsonArray("attrSubCmds"));
        } catch (RuntimeException error) {
            sendError(session, 1, "Failed to parse the payload: " + error.getMessage());
        }
    }

    private void handleTelemetryCommands(WebSocketSession session, JsonArray commands) throws IOException {
        if (commands == null) {
            return;
        }
        for (JsonElement element : commands) {
            JsonObject command = element.getAsJsonObject();
            int cmdId = command.get("cmdId").getAsInt();
            if (command.has("unsubscribe") && command.get("unsubscribe").getAsBoolean()) {
                sessions.get(session.getId()).subscriptions.remove(cmdId);
                sendUpdate(session, cmdId, Map.of());
                continue;
            }
            sessions.get(session.getId()).subscriptions.put(cmdId, command.deepCopy());
            JsonObject data = new JsonObject();
            String keys = command.has("keys") ? command.get("keys").getAsString() : "";
            for (String key : keys.split(",")) {
                if (!key.isBlank()) {
                    JsonArray values = new JsonArray();
                    JsonArray value = new JsonArray();
                    value.add(System.currentTimeMillis());
                    value.add("demo-" + key);
                    values.add(value);
                    data.add(key, values);
                }
            }
            sendUpdate(session, cmdId, data);
        }
    }

    private void handleAttributeCommands(WebSocketSession session, JsonArray commands) throws IOException {
        if (commands == null) {
            return;
        }
        for (JsonElement element : commands) {
            JsonObject command = element.getAsJsonObject();
            int cmdId = command.get("cmdId").getAsInt();
            sendUpdate(session, cmdId, Map.of("firmware", java.util.List.of(java.util.List.of(
                    System.currentTimeMillis(), "demo-1.0"))));
        }
    }

    private void sendUpdate(WebSocketSession session, int subscriptionId, Object data) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("subscriptionId", subscriptionId);
        response.addProperty("errorCode", 0);
        response.addProperty("errorMsg", "");
        response.add("data", JsonParser.parseString(new com.google.gson.Gson().toJson(data)));
        session.sendMessage(new TextMessage(response.toString()));
    }

    private void sendError(WebSocketSession session, int cmdId, String error) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("subscriptionId", cmdId);
        response.addProperty("errorCode", 2);
        response.addProperty("errorMsg", error);
        session.sendMessage(new TextMessage(response.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionMetadata removed = sessions.remove(session.getId());
        if (removed != null) {
            System.out.println("[Spring WebSocket] session closed: " + session.getId());
        }
    }

    private static String queryParam(URI uri, String name) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            if (part.startsWith(name + "=")) {
                return part.substring(name.length() + 1);
            }
        }
        return null;
    }

    private static final class SessionMetadata {
        private final String sessionId;
        private final String token;
        private final Map<Integer, JsonObject> subscriptions = new ConcurrentHashMap<>();

        private SessionMetadata(String sessionId, String token) {
            this.sessionId = sessionId;
            this.token = token;
        }
    }
}
