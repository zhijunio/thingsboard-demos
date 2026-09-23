# ThingsBoard Transport Service 示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard Transport Service 的凭证校验、telemetry/attributes 消息转换、Rule Engine 分流、Core 消息和 Device Actor 分流。

## 演示内容

- Transport Service API 请求
- MQTT 基础凭证校验方向
- `SessionInfoProto`
- telemetry 转换为 `TbMsg`
- attributes 转换和 scope metadata
- Rule Engine 队列消息
- Core 队列消息
- Device Actor 分流
- 内存 request/response queue

## 对应 ThingsBoard 源码

- `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`
- `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/service/DefaultTransportService.java`
- `common/message/src/main/proto/transport.proto`
- `application/src/main/java/org/thingsboard/server/actors/device/DeviceActor.java`

本项目使用内存队列替代 Kafka，保留 Transport Service 到 Rule Engine、Core 和 Device Actor 的消息方向。

## 运行

```bash
cd thingsboard-transport-service-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.transportdemo.TransportServiceDemo
```

程序会依次演示凭证校验、telemetry、attributes、session event、RPC 订阅和 Core 消息处理。
