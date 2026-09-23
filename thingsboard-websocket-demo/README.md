# ThingsBoard WebSocket 示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard Spring WebSocket 服务端、WebSocket session、消息处理和 telemetry subscription command。

## 演示内容

- Spring WebSocket 服务端注册
- `TextWebSocketHandler`
- WebSocket session 建立和关闭
- JSON 命令解析
- telemetry subscription command
- WebSocket 客户端连接
- 真实 ThingsBoard WebSocket endpoint 连接方式

## 对应 ThingsBoard 源码

- `application/src/main/java/org/thingsboard/server/controller/plugin/TbWebSocketHandler.java`
- `application/src/main/java/org/thingsboard/server/config/WebSocketConfiguration.java`
- `application/src/test/java/org/thingsboard/server/controller/plugin/TbWebSocketHandlerTest.java`

本项目保留 Spring WebSocket 服务端的连接和消息处理方向，客户端使用 Java-WebSocket，消息 payload 使用 Gson。

## 运行测试

```bash
cd thingsboard-websocket-demo
mvn clean test
```

## 运行本地 Spring WebSocket 服务端

```bash
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.websocketdemo.WebSocketServerDemo
```

## 连接真实 ThingsBoard

设置设备 access token 和 device id：

```bash
export TB_ACCESS_TOKEN=YOUR_DEVICE_ACCESS_TOKEN
export TB_DEVICE_ID=YOUR_DEVICE_ID
export TB_WS_ENDPOINT=ws://localhost:8080/api/ws/plugins/telemetry
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.websocketdemo.WebSocketDemo
```

客户端会发送 telemetry subscription command，并等待服务端消息。

## 设计边界

示例不包含完整的 ThingsBoard WebSocket 权限、订阅策略、遥测查询和设备会话管理；这些部分使用内存 handler 简化。
