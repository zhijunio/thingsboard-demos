# ThingsBoard Queue 示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard 队列抽象、Protobuf 队列消息、Producer/Consumer、Request/Response 和 Kafka 实现。

## 演示内容

- `TbQueueProducer`
- `TbQueueConsumer`
- `TbQueueMsg`
- `TopicPartitionInfo`
- Protobuf 消息编码和解码
- Producer/Consumer 单向消息
- Request/Response 请求响应模板
- requestId、responseTopic 和超时
- 内存 Queue 实现
- Kafka Producer/Consumer 实现
- Testcontainers Kafka 集成测试

## 对应 ThingsBoard 源码

- `common/queue/src/main/java/org/thingsboard/server/queue/TbQueueProducer.java`
- `common/queue/src/main/java/org/thingsboard/server/queue/TbQueueConsumer.java`
- `common/queue/src/main/java/org/thingsboard/server/queue/common/DefaultTbQueueRequestTemplate.java`
- `common/queue/src/main/java/org/thingsboard/server/queue/common/TbProtoQueueMsg.java`
- `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TopicPartitionInfo.java`

## 运行内存 Queue 示例

```bash
cd thingsboard-queue-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.queuedemo.QueueDemo
```

## 运行 Kafka 示例

启动 Kafka：

```bash
docker compose up -d
```

运行 Kafka 收发：

```bash
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.queuedemo.KafkaQueueDemo
```

默认连接 `localhost:9092`，也可以通过 `KAFKA_BOOTSTRAP_SERVERS` 修改。

停止 Kafka：

```bash
docker compose down
```

Kafka 集成测试使用 Testcontainers；没有 Docker 时，测试按配置跳过。

## 消息流程

```text
Producer
    -> TbQueueMsg
    -> TopicPartitionInfo
    -> Queue
    -> Consumer
    -> Protobuf decode
```

Request/Response 在消息 header 中传递 requestId 和 responseTopic，并在超时后移除 pending request。
