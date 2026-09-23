package org.example.thingsboard.websocketdemo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** 对应 ThingsBoard application/.../config/WebSocketConfiguration。 */
@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
    public static final String WS_API_ENDPOINT = "/api/ws";
    public static final String WS_PLUGINS_ENDPOINT = "/api/ws/plugins/";

    private final TbWebSocketHandler handler;

    public WebSocketConfiguration(TbWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/ws/**")
                .setAllowedOriginPatterns("*");
    }
}
