# ThingsBoard 设备会话示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard 设备会话、Device Actor、会话缓存、telemetry、server-side RPC 和会话超时。

## 演示内容

- 设备 token 认证
- 创建设备会话
- 会话所属节点信息
- 设备 Actor 消息处理
- telemetry 事件
- RPC 订阅
- server-side RPC 下发
- 会话活动时间更新
- 会话超时检查
- 会话缓存删除和超时事件

## 对应 ThingsBoard 源码

- `application/src/main/java/org/thingsboard/server/actors/device/DeviceActor.java`
- `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`
- `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorCreator.java`
- `application/src/main/java/org/thingsboard/server/service/session/DeviceSessionCacheService.java`
- `common/message/src/main/java/org/thingsboard/server/common/msg/timeout/DeviceActorServerSideRpcTimeoutMsg.java`

本项目保留 Device Actor 和会话消息的主要方向，使用内存 token、会话缓存和事件 sink 替代真实 transport、Redis 和数据库。

## 运行

```bash
cd thingsboard-device-session-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.devicesession.DeviceSessionDemo
```

输出会覆盖：

```text
CONNECTED
TELEMETRY
RPC
SESSION_TIMEOUT
```

## 会话流程

```text
设备连接
    -> token 校验
    -> DeviceSession 创建
    -> telemetry / RPC 消息进入 DeviceActor
    -> 活动时间更新
    -> 超时检查
    -> 会话关闭和缓存删除
```
