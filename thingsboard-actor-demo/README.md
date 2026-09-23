# ThingsBoard Actor System 示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard Actor System 的 Actor、Mailbox、Dispatcher、消息串行处理、初始化重试和停止通知。示例把 Actor 建模为一个设备，演示连接、遥测和 RPC 消息如何更新设备状态。

## 演示内容

- 创建 root actor 和 child actor
- Dispatcher 与线程池
- Mailbox 按顺序处理消息
- high priority 消息
- Actor 初始化失败后的重试
- 普通消息处理失败后的恢复策略
- 向子 Actor 广播消息
- Actor 停止时通知排队消息
- 设备连接、遥测和 RPC 状态更新

## 对应 ThingsBoard 源码

- `common/actor/src/main/java/org/thingsboard/server/actors/TbActorSystem.java`
- `common/actor/src/main/java/org/thingsboard/server/actors/DefaultTbActorSystem.java`
- `common/actor/src/main/java/org/thingsboard/server/actors/TbActorMailbox.java`
- `common/actor/src/main/java/org/thingsboard/server/actors/TbActor.java`

本项目保留核心 Actor 调用方向，使用 `DemoActor` 和 `DeviceState` 替代真实的 `DeviceActor` 与持久化服务。

## 运行

```bash
cd thingsboard-actor-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.actordemo.ActorDemo
```

程序会输出消息类型、最大并发数、失败次数、子 Actor 广播结果、session、telemetry、RPC 状态和停止原因。

## 设计边界

示例不包含完整的 ThingsBoard 设备、租户和规则链业务，只用一个简化设备状态演示 Actor System。线程池、消息队列和 Actor 状态均在进程内运行。
