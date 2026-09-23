package org.example.thingsboard.queuedemo;

import org.example.thingsboard.queuedemo.proto.QueueDemoProtos;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.memory.DefaultInMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueueDemoTest {
    @Test
    void protobufMessageTravelsThroughProducerAndConsumer() throws Exception {
        DefaultInMemoryStorage storage = new DefaultInMemoryStorage();
        InMemoryTbQueueProducer<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> producer =
                new InMemoryTbQueueProducer<>(storage, "telemetry");
        InMemoryTbQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> consumer =
                new InMemoryTbQueueConsumer<>(storage, "telemetry");
        consumer.subscribe();
        QueueDemoProtos.Telemetry source = QueueDemoProtos.Telemetry.newBuilder()
                .setDeviceName("device-1").setTemperature(18.5).build();

        producer.send(new TopicPartitionInfo("telemetry", null),
                new TbProtoQueueMsg<>(UUID.randomUUID(), source), TbQueueCallback.EMPTY);

        var actual = consumer.poll(0).get(0).getValue();
        assertEquals(source, actual);
    }

    @Test
    void requestResponseCompletesByRequestIdHeader() throws Exception {
        DefaultInMemoryStorage storage = new DefaultInMemoryStorage();
        var requestProducer = new InMemoryTbQueueProducer<TbProtoQueueMsg<QueueDemoProtos.DemoRequest>>(storage, "requests");
        var responseConsumer = new InMemoryTbQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.DemoResponse>>(storage, "responses");
        var requestConsumer = new InMemoryTbQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.DemoRequest>>(storage, "requests");
        var responseProducer = new InMemoryTbQueueProducer<TbProtoQueueMsg<QueueDemoProtos.DemoResponse>>(storage, "responses");
        var template = new DefaultTbQueueRequestTemplate<>(requestProducer, responseConsumer, 1000, 4, 5);
        template.init();
        requestConsumer.subscribe();

        var future = template.send(new TbProtoQueueMsg<>(UUID.randomUUID(),
                QueueDemoProtos.DemoRequest.newBuilder().setPayload("ping").build()));
        var request = requestConsumer.poll(0).get(0);
        var response = QueueDemoProtos.DemoResponse.newBuilder().setResult("pong").build();
        var headers = new org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders();
        request.getHeaders().getData().forEach((key, value) -> headers.put(key, value));
        responseProducer.send(new TopicPartitionInfo("responses", null),
                new TbProtoQueueMsg<>(request.getKey(), response, headers), TbQueueCallback.EMPTY);

        assertEquals("pong", future.get().getValue().getResult());
        assertEquals(0, template.pendingRequestCount());
        template.stop();
    }

    @Test
    void requestTimesOutAndIsRemovedFromPendingRequests() throws Exception {
        DefaultInMemoryStorage storage = new DefaultInMemoryStorage();
        var requestProducer = new InMemoryTbQueueProducer<TbProtoQueueMsg<QueueDemoProtos.DemoRequest>>(storage, "requests");
        var responseConsumer = new InMemoryTbQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.DemoResponse>>(storage, "responses");
        var template = new DefaultTbQueueRequestTemplate<>(requestProducer, responseConsumer, 20, 4, 5);
        template.init();

        var future = template.send(new TbProtoQueueMsg<>(UUID.randomUUID(),
                QueueDemoProtos.DemoRequest.newBuilder().setPayload("no-response").build()));
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> future.get(1, TimeUnit.SECONDS));
        assertInstanceOf(TimeoutException.class, exception.getCause());
        assertEquals(0, template.pendingRequestCount());
        template.stop();
    }
}
