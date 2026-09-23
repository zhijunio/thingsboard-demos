package org.example.thingsboard.queuedemo;

import org.example.thingsboard.queuedemo.proto.QueueDemoProtos;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.kafka.ProtobufKafkaDecoder;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaQueueConsumer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers(disabledWithoutDocker = true)
class KafkaQueueIntegrationTest {
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    @Test
    void protobufMessageTravelsThroughRealKafka() {
        String topic = "queue-demo-test-" + UUID.randomUUID();
        TbKafkaProducerTemplate<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> producer =
                new TbKafkaProducerTemplate<>(KAFKA.getBootstrapServers(), topic, "test-producer");
        TbKafkaQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> consumer =
                new TbKafkaQueueConsumer<>(KAFKA.getBootstrapServers(), topic, "test-consumer-" + UUID.randomUUID(),
                        "test-consumer", new ProtobufKafkaDecoder<>(QueueDemoProtos.Telemetry.parser()));
        try {
            consumer.subscribe();
            QueueDemoProtos.Telemetry expected = QueueDemoProtos.Telemetry.newBuilder()
                    .setDeviceName("kafka-test-device")
                    .setTs(123)
                    .setTemperature(19.5)
                    .build();
            producer.send(new TopicPartitionInfo(topic, null),
                    new TbProtoQueueMsg<>(UUID.randomUUID(), expected), TbQueueCallback.EMPTY);

            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                var messages = consumer.poll(500);
                if (!messages.isEmpty()) {
                    assertEquals(expected, messages.get(0).getValue());
                    consumer.commit();
                    return;
                }
            }
            fail("Kafka message was not received before timeout");
        } finally {
            consumer.unsubscribe();
            producer.stop();
        }
    }
}
