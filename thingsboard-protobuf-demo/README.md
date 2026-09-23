# ThingsBoard Protobuf 消息模型示例

这是一个独立的 Java/Maven 示例，用来演示 ThingsBoard 消息队列相关代码使用的 Protobuf 消息模型。

## 示例对应源码

项目中的协议文件是 ThingsBoard 源码中相关定义的精简版：

- `common/message/src/main/proto/tbmsg.proto`
- `common/proto/src/main/proto/queue.proto`

示例保留了核心消息的包名、消息名、字段编号和字段类型，删除了与本例无关的大量业务消息，方便阅读。具体包括：

- `TbMsgProto`：消息信封，保存消息 ID、消息类型、实体信息、元数据和业务数据。
- `PostTelemetryMsg`：遥测数据载荷。
- `TsKvListProto`：某个时间戳下的一组键值数据。
- `KeyValueProto`：具体的遥测键值，例如温度和湿度。

## 示例流程

程序执行以下流程：

1. 构造一个包含温度和湿度的 `PostTelemetryMsg`。
2. 将 telemetry payload 序列化为 Protobuf 二进制数据，再反序列化回来。
3. 构造 `TbMsgProto` 消息信封，并加入设备信息和元数据。
4. 按 ThingsBoard 的实际模型，将 JSON 字符串写入 `TbMsgProto.data`。该字段在源码中是 `string`，不是嵌套的 Protobuf 字段。
5. 将整个 `TbMsgProto` 序列化为二进制数据。
6. 从二进制数据恢复 `TbMsgProto`，读取消息类型、设备元数据和业务字段。

## 运行环境

- JDK 17 或更高版本
- Maven 3.9 或更高版本

## 运行

```bash
cd ~/github/thingsboard-demos/thingsboard-protobuf-demo
mvn clean test
mvn compile exec:java
```

程序会输出类似信息：

```text
message type: POST_TELEMETRY
message id: demo-message-001
device: demo-device
telemetry points: 2
telemetry bytes: 51
serialized bytes: 148
```

其中 `telemetry bytes` 是 `PostTelemetryMsg` 的 Protobuf 二进制大小，`serialized bytes` 是包含信封和元数据的 `TbMsgProto` 大小。

## 目录结构

```text
src/main/proto/
  tbmsg.proto       # 消息信封模型
  queue.proto       # 遥测键值模型
src/main/java/
  .../ProtobufDemo.java
src/test/java/
  .../ProtobufDemoTest.java
```
