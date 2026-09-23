package org.example.thingsboard.websocketdemo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 对应 ThingsBoard 测试代码中的 WebSocket client：连接、发送订阅命令、接收文本消息、关闭。
 */
public final class ThingsBoardWebSocketClient extends WebSocketClient implements AutoCloseable {
    private final BlockingQueue<JsonObject> messages = new LinkedBlockingQueue<>();

    public ThingsBoardWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("[WebSocket] connected: " + getURI());
    }

    @Override
    public void onMessage(String text) {
        JsonObject message = JsonParser.parseString(text).getAsJsonObject();
        messages.offer(message);
        System.out.println("[WebSocket] received: " + text);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WebSocket] closed: code=" + code + ", reason=" + reason);
    }

    @Override
    public void onError(Exception error) {
        System.err.println("[WebSocket] error: " + error.getMessage());
    }

    public JsonObject awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
        JsonObject message = messages.poll(timeout, unit);
        if (message == null) {
            throw new IllegalStateException("没有在超时时间内收到 ThingsBoard WebSocket 消息");
        }
        return message;
    }

    @Override
    public void close() {
        super.close();
    }
}
