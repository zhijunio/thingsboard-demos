# ThingsBoard 集群通信示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard 集群服务、节点注册、集群广播、定向消息转发、hash partition 和拓扑变化。

## 演示内容

- `TbClusterService` 风格的集群服务边界
- 两个进程内集群节点注册
- `TopicPartitionInfo` 目标 partition 和节点归属
- `HashPartitionService` 风格的实体 hash 路由
- 定向消息转发到 partition owner
- 广播 Core 事件
- 广播缓存失效事件
- 节点下线后的发送失败回调
- `PartitionChangeEvent`
- `ClusterTopologyChangeEvent`
- `ServiceListChangedEvent`

## 对应 ThingsBoard 源码

- `common/cluster-api/src/main/java/org/thingsboard/server/cluster/TbClusterService.java`
- `application/src/main/java/org/thingsboard/server/service/queue/DefaultTbClusterService.java`
- `common/queue/src/main/java/org/thingsboard/server/queue/discovery/HashPartitionService.java`
- `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`
- `common/queue/src/main/java/org/thingsboard/server/queue/discovery/event/PartitionChangeEvent.java`
- `common/queue/src/main/java/org/thingsboard/server/queue/discovery/event/ClusterTopologyChangeEvent.java`

真实 ThingsBoard 使用服务发现、通知 topic 和队列；本项目使用 `InMemoryClusterBus` 替代这些基础设施。

## 运行

```bash
cd thingsboard-cluster-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.cluster.ClusterDemo
```

程序会演示：

1. `node-a` 和 `node-b` 注册。
2. 通过 hash partition 找到实体 owner。
3. `node-a` 向 `node-b` 转发定向消息。
4. 两个节点收到广播和缓存失效事件。
5. `node-b` 下线后发送失败。
6. 节点拓扑变化后产生 partition change 事件。

## 设计边界

这是进程内双节点学习示例，不包含真实 Kafka、Redis、ZooKeeper、网络通信和服务发现。
