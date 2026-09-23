# ThingsBoard MQTT 设备示例

这是一个独立的 Java/Maven MQTT 设备客户端示例，参考 ThingsBoard 源码中的 MQTT 设备接入实现和集成测试。

## 演示内容

默认运行会完成以下操作：

1. 使用设备 access token 连接 ThingsBoard MQTT 服务。
2. 订阅 server-side RPC：`v1/devices/me/rpc/request/+`。
3. 发布设备 attributes：`v1/devices/me/attributes`。
4. 发布 JSON telemetry：`v1/devices/me/telemetry`。
5. 收到 server-side RPC 后，发布对应的 response。

还可以使用 `--proto` 发送 Protobuf telemetry，演示 ThingsBoard 中“Device Profile 配置 Protobuf schema，设备发送二进制 payload”的模式。

## 对应 ThingsBoard 源码

主要参考：

- `common/data/src/main/java/org/thingsboard/server/common/data/device/profile/MqttTopics.java`
- `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`
- `application/src/test/java/org/thingsboard/server/transport/mqtt/mqttv3/telemetry/timeseries/AbstractMqttTimeseriesProtoIntegrationTest.java`
- `application/src/test/java/org/thingsboard/server/transport/mqtt/mqttv3/rpc/AbstractMqttServerSideRpcIntegrationTest.java`

本项目使用的主要 topic：

| 方向 | Topic | Payload |
| --- | --- | --- |
| 设备 -> ThingsBoard | `v1/devices/me/telemetry` | JSON telemetry |
| 设备 -> ThingsBoard | `proto/telemetry` | Protobuf telemetry，需配置 Profile |
| 设备 -> ThingsBoard | `v1/devices/me/attributes` | JSON attributes |
| ThingsBoard -> 设备 | `v1/devices/me/rpc/request/+` | server-side RPC request |
| 设备 -> ThingsBoard | `v1/devices/me/rpc/response/{requestId}` | RPC response |

## 运行环境

- JDK 17 或更高版本
- Maven 3.9 或更高版本
- 运行中的 ThingsBoard MQTT 服务，默认地址为 `tcp://localhost:1883`
- 一个已经创建并启用的 ThingsBoard 设备及其 access token

## 不连接服务器查看 payload

没有 ThingsBoard 或设备 token 时，可以先运行 dry-run：

```bash
cd thingsboard-mqtt-demo
mvn compile exec:java -Dexec.args="--dry-run"
```

该模式会输出 JSON telemetry、attributes、RPC topic、Protobuf schema 和 Protobuf 二进制数据的十六进制表示。

## 连接 ThingsBoard 运行

直接通过命令行传入 broker 和 token：

```bash
mvn compile exec:java \
  -Dexec.args="--broker tcp://localhost:1883 --token YOUR_DEVICE_ACCESS_TOKEN --duration 60"
```

也可以使用环境变量，避免把 token 放进 shell 历史或命令行参数：

```bash
export TB_MQTT_BROKER=tcp://localhost:1883
export TB_DEVICE_TOKEN=YOUR_DEVICE_ACCESS_TOKEN
mvn compile exec:java -Dexec.args="--duration 60"
```

TLS broker 使用 `ssl://` URI，例如：

```bash
mvn compile exec:java \
  -Dexec.args="--broker ssl://localhost:8883 --token YOUR_DEVICE_ACCESS_TOKEN"
```

TLS 证书必须由 JVM 默认 trust store 信任；本示例不会关闭证书校验，也不会实现不安全的 trust-all SSL 配置。

## 发送 Protobuf telemetry

运行：

```bash
mvn compile exec:java \
  -Dexec.args="--broker tcp://localhost:1883 --token YOUR_DEVICE_ACCESS_TOKEN --proto"
```

`--proto` 默认发送到 `proto/telemetry`。在 ThingsBoard 中，需要先为设备使用的 Device Profile 配置：

- MQTT transport payload type：`Protobuf`。
- Telemetry topic filter：`proto/telemetry`。
- Telemetry Protobuf schema：项目中的 [`src/main/proto/telemetry.proto`](src/main/proto/telemetry.proto)。

源码测试也采用同样的思路：先在设备 Profile 配置 `TransportPayloadType.PROTOBUF` 和 `telemetryProtoSchema`，再向配置的 topic 发送 `DynamicMessage.toByteArray()` 产生的二进制 payload。

## Server-side RPC

设备连接后会订阅：

```text
v1/devices/me/rpc/request/+
```

可以从 ThingsBoard UI 或 REST API 向该设备发送 RPC。客户端收到请求后会：

1. 从 topic 最后一段读取 `requestId`。
2. 打印 RPC request。
3. 发布到 `v1/devices/me/rpc/response/{requestId}`。

示例 response：

```json
{"success":true,"echo":"handled by thingsboard-mqtt-demo"}
```

## 测试和限制

执行本地 payload 测试：

```bash
mvn clean test
```

测试覆盖 Protobuf payload 的序列化/反序列化和 JSON payload 的关键字段。项目不会自动创建 ThingsBoard 设备、Device Profile 或 access token；这些资源需要先在 ThingsBoard 中准备好。
