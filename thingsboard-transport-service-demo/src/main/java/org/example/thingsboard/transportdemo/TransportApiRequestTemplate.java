package org.example.thingsboard.transportdemo;

import com.google.protobuf.GeneratedMessageV3;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 对应 DefaultTbQueueRequestTemplate 的请求/响应关联逻辑。
 * ThingsBoard 使用 queue header 传递 request id；本示例直接复用 TbProtoQueueMsg.key。
 */
public final class TransportApiRequestTemplate {
    private final InMemoryTransportQueueFactory queues;
    private final ConcurrentMap<UUID, CompletableFuture<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>>> pending =
            new ConcurrentHashMap<>();

    public TransportApiRequestTemplate(InMemoryTransportQueueFactory queues) {
        this.queues = queues;
    }

    public CompletableFuture<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> send(
            TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg> request) {
        CompletableFuture<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> future = new CompletableFuture<>();
        if (pending.putIfAbsent(request.getKey(), future) != null) {
            future.completeExceptionally(new IllegalStateException("duplicate request key: " + request.getKey()));
            return future;
        }
        queues.sendTransportApiRequest(request);
        return future;
    }

    public void respond(UUID requestKey, TransportProtos.TransportApiResponseMsg response) {
        CompletableFuture<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> future = pending.remove(requestKey);
        if (future != null) {
            future.complete(new TbProtoQueueMsg<>(requestKey, response));
        }
    }

    public int pendingRequestCount() {
        return pending.size();
    }
}
