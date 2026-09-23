package org.thingsboard.server.queue.common;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 对应 ThingsBoard DefaultTbQueueRequestTemplate 的最小实现：
 * requestId 写入 header，响应消费者取出同一 requestId 后完成 Future。
 */
public final class DefaultTbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg>
        extends AbstractTbQueueTemplate implements TbQueueRequestTemplate<Request, Response> {
    private final TbQueueProducer<Request> requestProducer;
    private final TbQueueConsumer<Response> responseConsumer;
    private final long maxRequestTimeoutMs;
    private final long maxPendingRequests;
    private final long pollIntervalMs;
    private final ConcurrentMap<UUID, PendingResponse<Response>> pendingRequests = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean stopped;

    public DefaultTbQueueRequestTemplate(TbQueueProducer<Request> requestProducer,
                                         TbQueueConsumer<Response> responseConsumer,
                                         long maxRequestTimeoutMs,
                                         long maxPendingRequests,
                                         long pollIntervalMs) {
        this.requestProducer = requestProducer;
        this.responseConsumer = responseConsumer;
        this.maxRequestTimeoutMs = maxRequestTimeoutMs;
        this.maxPendingRequests = maxPendingRequests;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Override
    public void init() {
        responseConsumer.subscribe();
        executor.submit(this::mainLoop);
    }

    private void mainLoop() {
        while (!stopped) {
            List<Response> responses = responseConsumer.poll(pollIntervalMs);
            responses.forEach(this::processResponse);
            cleanupExpiredRequests();
            responseConsumer.commit();
        }
    }

    private void processResponse(Response response) {
        byte[] requestId = response.getHeaders().get(REQUEST_ID_HEADER);
        if (requestId == null) {
            return;
        }
        PendingResponse<Response> pending = pendingRequests.remove(bytesToUuid(requestId));
        if (pending != null) {
            pending.future().set(response);
        }
    }

    private void cleanupExpiredRequests() {
        long now = System.nanoTime();
        pendingRequests.forEach((requestId, pending) -> {
            if (pending.expireAtNs() <= now && pendingRequests.remove(requestId, pending)) {
                pending.future().setException(new TimeoutException("queue request timed out: " + requestId));
            }
        });
    }

    @Override
    public ListenableFuture<Response> send(Request request) {
        return send(request, TimeUnit.MILLISECONDS.toNanos(maxRequestTimeoutMs));
    }

    @Override
    public ListenableFuture<Response> send(Request request, Integer partition) {
        return send(request);
    }

    @Override
    public ListenableFuture<Response> send(Request request, long timeoutNs) {
        if (pendingRequests.size() >= maxPendingRequests) {
            return Futures.immediateFailedFuture(new IllegalStateException("pending request limit reached"));
        }
        UUID requestId = UUID.randomUUID();
        SettableFuture<Response> future = SettableFuture.create();
        request.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
        request.getHeaders().put(RESPONSE_TOPIC_HEADER, stringToBytes(responseConsumer.getTopic()));
        request.getHeaders().put(EXPIRE_TS_HEADER, stringToBytes(Long.toString(System.currentTimeMillis() + maxRequestTimeoutMs)));
        pendingRequests.put(requestId, new PendingResponse<>(future, System.nanoTime() + timeoutNs));
        requestProducer.send(new TopicPartitionInfo(requestProducer.getDefaultTopic(), null), request, new TbQueueCallback() {
            @Override
            public void onSuccess(org.thingsboard.server.queue.TbQueueMsgMetadata metadata) {
            }

            @Override
            public void onFailure(Throwable t) {
                PendingResponse<Response> pending = pendingRequests.remove(requestId);
                if (pending != null) {
                    pending.future().setException(t);
                }
            }
        });
        return future;
    }

    public int pendingRequestCount() {
        return pendingRequests.size();
    }

    private record PendingResponse<T>(SettableFuture<T> future, long expireAtNs) {
    }

    @Override
    public void stop() {
        stopped = true;
        responseConsumer.unsubscribe();
        requestProducer.stop();
        executor.shutdownNow();
    }
}
