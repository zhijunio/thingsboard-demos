# ThingsBoard gRPC 双向流示例

这是一个独立的 Java/Maven 示例，用来演示 ThingsBoard Edge 使用的 gRPC 双向流模式。

## 示例对应源码

示例参考 ThingsBoard 源码中的以下文件：

- `common/edge-api/src/main/proto/edge.proto`
- `common/edge-api/src/main/java/org/thingsboard/edge/rpc/EdgeGrpcClient.java`
- `application/src/main/java/org/thingsboard/server/service/edge/rpc/GrpcServer.java`
- `application/src/main/java/org/thingsboard/server/service/edge/rpc/session/EdgeGrpcSession.java`

核心 RPC 定义为：

```proto
service EdgeRpcService {
    rpc handleMsgs(stream RequestMsg) returns (stream ResponseMsg);
}
```

客户端和服务端都可以在同一条连接上持续发送消息：

```text
客户端                                     服务端
  |                                           |
  | -- CONNECT_RPC_MESSAGE ----------------> |
  | <--------------- ACCEPTED -------------- |
  | -- SYNC_REQUEST_RPC_MESSAGE ----------> |
  | <---------------- sync downlink -------- |
  | -- UPLINK_RPC_MESSAGE ----------------> |
  | <---------------- uplink ACK ------------ |
  |                                           |
```

## 示例流程

程序在同一个 JVM 中启动本地 gRPC 服务端和客户端：

1. 服务端使用 `NettyServerBuilder` 监听端口。
2. 客户端使用 `NettyChannelBuilder` 创建明文 channel。
3. 客户端通过生成的异步 stub 调用 `handleMsgs`，得到请求方向的 `StreamObserver`。
4. 客户端发送 `CONNECT_RPC_MESSAGE`，携带 Edge routing key、secret、版本和最大消息大小。
5. 服务端验证凭据，返回 `ACCEPTED` 和 `EdgeConfiguration`。
6. 客户端发送全量同步请求。
7. 客户端发送包含一个 telemetry 点的 uplink 消息。
8. 服务端分别返回同步完成 downlink 和 uplink ACK。

## 运行环境

- JDK 17 或更高版本
- Maven 3.9 或更高版本

## 运行

```bash
cd thingsboard-grpc-demo
mvn compile exec:java
```

默认监听 `7070` 端口。如果端口已被占用，可以传入其他端口：

```bash
mvn compile exec:java -Dexec.args="7071"
```

运行成功后，可以看到类似输出：

```text
[server] listening on port 7070
[server] connect key=demo-edge, accepted=true
[client] connect response=ACCEPTED
[server] sync request, fullSync=true
[client] downlink id=1
[server] uplink id=1001, entities=1
[client] uplink ack id=1001, success=true
```

## 重要说明

这是本地协议演示，不会连接真实的 ThingsBoard 实例。示例中的 Edge 凭据是写死的演示值：

```text
routing key: demo-edge
secret:      demo-secret
```

真实 Edge 连接还需要：

- ThingsBoard 中已经创建的 Edge 实体。
- 对应的 routing key 和 secret。
- ThingsBoard 服务端 Edge RPC 配置及端口。
- 完整且与服务端版本匹配的 `edge.proto`、`queue.proto` 定义。
- 生产环境下的 TLS 配置，而不是本示例使用的明文连接。

本项目中的协议文件是 `edge.proto` 和 `queue.proto` 的精简版，只保留了连接握手、同步、telemetry uplink 和 ACK 所需的字段；因此它适合学习 gRPC 调用流程，不适合直接替代 ThingsBoard Edge 客户端。
