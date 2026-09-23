# ThingsBoard 示例项目

这个目录用于保存 ThingsBoard 源码学习和行为验证项目。

这些项目不是 ThingsBoard 功能的替代实现，而是从 ThingsBoard 原仓库中提取关键类、消息模型和调用方向，删除数据库、完整配置系统、生产集群基础设施等与当前主题无关的部分，形成可以独立运行和阅读的最小示例。

## 示例列表

| 项目 | 学习内容 |
| --- | --- |
| [thingsboard-protobuf-demo](./thingsboard-protobuf-demo/) | Protobuf 消息定义、代码生成、二进制序列化与反序列化 |
| [thingsboard-grpc-demo](./thingsboard-grpc-demo/) | Edge gRPC 双向流、握手、同步、telemetry uplink 和 ACK |
| [thingsboard-mqtt-demo](./thingsboard-mqtt-demo/) | MQTT 设备连接、认证、生命周期、JSON/Protobuf telemetry 和 server-side RPC |
| [thingsboard-queue-demo](./thingsboard-queue-demo/) | `TbQueueProducer`、`TbQueueConsumer`、请求响应、Protobuf 消息和 Kafka |
| [thingsboard-transport-service-demo](./thingsboard-transport-service-demo/) | Transport Service 的连接、消息接收和向 Core 转发方向 |
| [thingsboard-websocket-demo](./thingsboard-websocket-demo/) | 参考 Spring WebSocket 服务端的连接、会话和消息处理 |
| [thingsboard-cache-consistency-demo](./thingsboard-cache-consistency-demo/) | 事务提交、业务数据更新、缓存失效和跨节点一致性 |
| [thingsboard-actor-demo](./thingsboard-actor-demo/) | `TbActorSystem`、Mailbox、消息串行处理、初始化重试和停止通知 |
| [thingsboard-rule-engine-demo](./thingsboard-rule-engine-demo/) | Rule Chain Actor、Rule Node Actor、关系路由和失败回调 |
| [thingsboard-device-session-demo](./thingsboard-device-session-demo/) | 设备会话创建、telemetry、RPC 和会话超时 |
| [thingsboard-cluster-demo](./thingsboard-cluster-demo/) | `TbClusterService`、集群广播、定向转发、hash partition 和拓扑变更 |

每个项目都是独立的 Maven 项目，可以单独进入项目目录运行，不依赖 ThingsBoard 主工程。

## 源码对应关系

示例优先保留 ThingsBoard 中的类名、消息名、字段编号、模块边界和调用方向。具体项目的 README 会列出对应的 ThingsBoard 源码文件。

整体关系可以概括为：

```text
设备连接 / gRPC / WebSocket
          |
          v
Transport Service
          |
          v
TbClusterService / Queue Producer
          |
          v
Core / Rule Engine / Actor
          |
          v
业务数据、缓存和事件
```

消息模型和传输方式的关系：

```text
Protobuf
   |
   +-- Queue 消息模型
   |
   +-- gRPC 请求和响应模型
   |
   +-- MQTT Protobuf payload
```

集群示例中的消息路由关系：

```text
业务服务
    |
    v
TbClusterService
    |
    v
Cluster Bus / Queue Producer
    |
    +-- 本节点处理
    |
    +-- 按 TopicPartitionInfo 转发到目标节点
    |
    +-- 广播到所有活动节点
```

## 运行环境

- JDK 17 或更高版本
- Maven 3.9 或更高版本
- `thingsboard-mqtt-demo` 需要可连接的 MQTT Broker；详细配置见该项目 README
- `thingsboard-queue-demo` 的 Kafka 集成测试需要 Docker；没有 Docker 时，Testcontainers 测试会按项目配置跳过

## 运行示例

以集群示例为例：

```bash
cd ~/github/thingsboard-demos/thingsboard-cluster-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.cluster.ClusterDemo
```

程序会演示：

1. `node-a` 和 `node-b` 注册到集群。
2. 通过 hash partition 找到实体所属节点。
3. `node-a` 使用 `TbClusterService` 向 `node-b` 转发定向消息。
4. 广播 Core 事件和缓存失效事件。
5. `node-b` 下线后，向旧目标发送消息并触发失败回调。
6. 节点拓扑变化后产生 partition change 事件。

运行单个项目时，通常使用：

```bash
cd ~/github/thingsboard-demos/<项目目录>
mvn clean test
```

具体的启动参数、外部服务和验证流程以各项目自己的 README 为准。

## 设计边界

示例中的内存实现只用于替代真实基础设施：

- 内存 Queue 用于替代 Kafka 等消息队列。
- 进程内 Cluster Bus 用于替代真实服务发现和集群通知 topic。
- 内存 Map 用于替代数据库和缓存。
- 简化的实体、租户和消息类型只保留当前示例需要的字段。

这意味着示例可以帮助理解 ThingsBoard 的源码结构和运行方向，但不能直接作为生产实现或 ThingsBoard 协议的完整实现。

## 参考资料

- [ThingsBoard 源码](https://github.com/thingsboard/thingsboard)
- 各示例目录中的 README、源码和测试用例
