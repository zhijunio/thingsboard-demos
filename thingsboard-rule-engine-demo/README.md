# ThingsBoard Rule Engine 示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard Rule Engine 的核心消息流和规则节点路由。

示例保留 `RuleChainActor`、`RuleNodeActor`、`RuleNodeRelation`、`TbMsg` 以及 Actor mailbox 的主要调用方向，只使用内存规则链配置和两个简单规则节点，省略数据库、集群队列和完整规则节点配置系统。

## 演示内容

- `QueueToRuleEngineMsg` 进入规则链 Actor
- Rule Chain Actor 把消息投递给首个 Rule Node
- Rule Node 通过 `tellNext` 返回关系类型
- Rule Chain Actor 根据 `Success` / `Failure` 路由消息
- 消息在多个 Rule Node Actor 之间异步传递
- 规则节点处理失败后通过 callback 返回结果
- 规则节点执行次数计数
- telemetry 阈值判断

## 对应 ThingsBoard 源码

主要参考：

- `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainActor.java`
- `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainActorMessageProcessor.java`
- `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleNodeActor.java`
- `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleNodeActorMessageProcessor.java`
- `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleNodeRelation.java`
- `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainToRuleNodeMsg.java`
- `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleNodeToRuleChainTellNextMsg.java`
- `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleNode.java`

## 消息流程

```text
QueueToRuleEngineMsg
    |
    v
RuleChainActor
    |
    v
MessageTypeFilterNode
    |
    +-- Success --> TelemetryThresholdNode
    |                    |
    |                    +-- Success --> callback.onSuccess()
    |                    +-- Failure --> callback.onFailure()
    |
    +-- Failure --> callback.onFailure()
```

这对应 ThingsBoard 中的核心方向：

```text
队列消费者
    -> RuleChainActor
    -> RuleNodeActor
    -> RuleNodeContext.tellNext(...)
    -> RuleChainActor 路由
    -> 下一个 RuleNode 或处理完成
```

## 规则链配置

演示程序在内存中定义一条规则链：

```text
Message Type Filter
    -- Success -->
Temperature >= 20
```

规则节点行为：

- `MessageTypeFilterNode`：只允许 `POST_TELEMETRY` 消息通过。
- `TelemetryThresholdNode`：要求 `temperature >= 20`。
- 没有后继节点的 `Success` 关系结束为成功。
- 没有后继节点的 `Failure` 关系结束为失败。

## 运行环境

- JDK 17 或更高版本
- Maven 3.9 或更高版本

## 运行测试

```bash
cd thingsboard-rule-engine-demo
mvn clean test
```

测试覆盖：

1. `POST_TELEMETRY` 且温度为 25 时经过两个节点并成功。
2. 温度为 10 时在阈值节点返回失败。
3. `POST_ATTRIBUTES` 在类型过滤节点返回失败，不会进入阈值节点。

## 运行演示程序

```bash
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.ruleengine.RuleEngineDemo
```

示例输出：

```text
规则链已提交 3 条消息，结果通过异步 callback 输出。
[RuleEngine] type=POST_ATTRIBUTES, temperature=25.0, result=FAILURE, reason=rule node returned failure
[RuleEngine] type=POST_TELEMETRY, temperature=25.0, result=SUCCESS
[RuleEngine] type=POST_TELEMETRY, temperature=10.0, result=FAILURE, reason=rule node returned failure
```

输出顺序可能因 Actor 异步调度而变化。

## 与完整 ThingsBoard 的差异

本示例没有实现：

- 真实的 Kafka 或其他消息队列
- `TbMsg` 的完整字段和 Protobuf 序列化
- Rule Chain、Rule Node 的数据库配置
- Spring Bean 和 Rule Node 反射加载
- 完整的 `TbContext`、callback 和 API usage 统计
- 多租户、分区、集群转发
- Rule Chain 到 Rule Chain 的跳转
- 多个目标节点的 callback 聚合
- JavaScript/TBEL 等脚本节点

示例只保留规则引擎最重要的执行路径：消息进入规则链、节点处理、关系返回和继续路由。
