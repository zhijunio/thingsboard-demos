package org.example.thingsboard.queuedemo;

import org.example.thingsboard.queuedemo.proto.QueueDemoProtos;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.memory.DefaultInMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public final class QueueDemo {
    private QueueDemo() {
    }

    public static void main(String[] args) throws Exception {
        DefaultInMemoryStorage storage = new DefaultInMemoryStorage();
        demonstrateOneWayProducerConsumer(storage);
        demonstrateRequestResponse(storage);
        System.out.println("完成：Producer/Consumer 和 Request/Response 两条 Queue 路径均已执行。");
    }

    private static void demonstrateOneWayProducerConsumer(DefaultInMemoryStorage storage) throws Exception {
        String topic = "demo.telemetry";
        InMemoryTbQueueProducer<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> producer =
                new InMemoryTbQueueProducer<>(storage, topic);
        InMemoryTbQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.Telemetry>> consumer =
                new InMemoryTbQueueConsumer<>(storage, topic);
        consumer.subscribe();

        QueueDemoProtos.Telemetry payload = QueueDemoProtos.Telemetry.newBuilder()
                .setDeviceName("demo-device")
                .setTs(1700000000000L)
                .setTemperature(23.5)
                .build();
        producer.send(new TopicPartitionInfo(topic, null),
                new TbProtoQueueMsg<>(UUID.randomUUID(), payload), new PrintingCallback("telemetry"));

        TbProtoQueueMsg<QueueDemoProtos.Telemetry> message = consumer.poll(0).get(0);
        QueueDemoProtos.Telemetry decoded = QueueDemoProtos.Telemetry.parseFrom(message.getData());
        System.out.println("[Producer/Consumer] topic=" + topic
                + ", device=" + decoded.getDeviceName()
                + ", temperature=" + decoded.getTemperature()
                + ", lag=" + storage.getLag(topic));
        consumer.commit();
    }

    private static void demonstrateRequestResponse(DefaultInMemoryStorage storage) throws Exception {
        String requestTopic = "demo.request";
        String responseTopic = "demo.response.node-1";
        InMemoryTbQueueProducer<TbProtoQueueMsg<QueueDemoProtos.DemoRequest>> requestProducer =
                new InMemoryTbQueueProducer<>(storage, requestTopic);
        InMemoryTbQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.DemoResponse>> responseConsumer =
                new InMemoryTbQueueConsumer<>(storage, responseTopic);
        InMemoryTbQueueConsumer<TbProtoQueueMsg<QueueDemoProtos.DemoRequest>> coreRequestConsumer =
                new InMemoryTbQueueConsumer<>(storage, requestTopic);
        InMemoryTbQueueProducer<TbProtoQueueMsg<QueueDemoProtos.DemoResponse>> coreResponseProducer =
                new InMemoryTbQueueProducer<>(storage, responseTopic);

        DefaultTbQueueRequestTemplate<TbProtoQueueMsg<QueueDemoProtos.DemoRequest>,
                TbProtoQueueMsg<QueueDemoProtos.DemoResponse>> template =
                new DefaultTbQueueRequestTemplate<>(requestProducer, responseConsumer, 2000, 16, 20);
        template.init();
        coreRequestConsumer.subscribe();

        TbProtoQueueMsg<QueueDemoProtos.DemoRequest> request = new TbProtoQueueMsg<>(
                UUID.randomUUID(), QueueDemoProtos.DemoRequest.newBuilder()
                        .setRequestId("application-request-1")
                        .setPayload("validate-device")
                        .build());
        var responseFuture = template.send(request);
        TbProtoQueueMsg<QueueDemoProtos.DemoRequest> coreMessage = coreRequestConsumer.poll(0).get(0);
        UUID requestId = requestId(coreMessage);
        String actualResponseTopic = new String(
                coreMessage.getHeaders().get("responseTopic"), StandardCharsets.UTF_8);
        System.out.println("[Request] requestId=" + requestId
                + ", responseTopic=" + actualResponseTopic
                + ", payload=" + coreMessage.getValue().getPayload());

        DefaultTbQueueMsgHeaders responseHeaders = copyHeaders(coreMessage);
        QueueDemoProtos.DemoResponse responsePayload = QueueDemoProtos.DemoResponse.newBuilder()
                .setRequestId(coreMessage.getValue().getRequestId())
                .setResult("accepted-by-core")
                .build();
        coreResponseProducer.send(new TopicPartitionInfo(actualResponseTopic, null),
                new TbProtoQueueMsg<>(coreMessage.getKey(), responsePayload, responseHeaders), TbQueueCallback.EMPTY);

        System.out.println("[Response] result=" + responseFuture.get().getValue().getResult()
                + ", pending=" + template.pendingRequestCount());
        template.stop();
    }

    private static UUID requestId(TbQueueMsg message) {
        byte[] bytes = message.getHeaders().get("requestId");
        if (bytes == null || bytes.length != 16) {
            throw new IllegalStateException("requestId header is missing");
        }
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static DefaultTbQueueMsgHeaders copyHeaders(TbQueueMsg message) {
        DefaultTbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        message.getHeaders().getData().forEach((key, value) -> headers.put(key, value.clone()));
        return headers;
    }

    private record PrintingCallback(String name) implements TbQueueCallback {
        @Override
        public void onSuccess(org.thingsboard.server.queue.TbQueueMsgMetadata metadata) {
            System.out.println("[Producer] " + name + " sent");
        }

        @Override
        public void onFailure(Throwable t) {
            throw new IllegalStateException("failed to send " + name, t);
        }
    }
}
