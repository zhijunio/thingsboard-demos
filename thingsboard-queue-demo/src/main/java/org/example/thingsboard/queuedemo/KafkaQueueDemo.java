package org.example.thingsboard.queuedemo;

import org.example.thingsboard.queuedemo.proto.QueueDemoProtos;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.kafka.ProtobufKafkaDecoder;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaQueueConsumer;

import java.util.UUID;

/** 使用 docker compose 中的 Kafka 运行真实队列收发。 */
public final class KafkaQueueDemo {
    private KafkaQueueDemo() {
    }

    public static void main(String[] args) {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String topic = "demo.kafka.telemetry";
        TbKafkaProducerTemplate<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> producer =
                new TbKafkaProducerTemplate<>(bootstrapServers, topic, "queue-demo-producer");
        TbKafkaQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> consumer =
                new TbKafkaQueueConsumer<>(bootstrapServers, topic, "queue-demo-consumer-" + UUID.randomUUID(),
                        "queue-demo-consumer", new ProtobufKafkaDecoder<>(QueueDemoProtos.Telemetry.parser()));
        try {
            consumer.subscribe();
            QueueDemoProtos.Telemetry payload = QueueDemoProtos.Telemetry.newBuilder()
                    .setDeviceName("kafka-device")
                    .setTs(System.currentTimeMillis())
                    .setTemperature(25.0)
                    .build();
            producer.send(new TopicPartitionInfo(topic, null),
                    new TbProtoQueueMsg<>(UUID.randomUUID(), payload), TbQueueCallback.EMPTY);

            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline) {
                var messages = consumer.poll(500);
                if (!messages.isEmpty()) {
                    System.out.println("[Kafka] topic=" + topic
                            + ", device=" + messages.get(0).getValue().getDeviceName()
                            + ", temperature=" + messages.get(0).getValue().getTemperature());
                    consumer.commit();
                    return;
                }
            }
            throw new IllegalStateException("Kafka message was not received before timeout");
        } finally {
            consumer.unsubscribe();
            producer.stop();
        }
    }
}
