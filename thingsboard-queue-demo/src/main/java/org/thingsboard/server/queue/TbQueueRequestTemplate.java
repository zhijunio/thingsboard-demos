package org.thingsboard.server.queue;

import com.google.common.util.concurrent.ListenableFuture;

public interface TbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> {
    void init();

    ListenableFuture<Response> send(Request request);

    ListenableFuture<Response> send(Request request, long timeoutNs);

    ListenableFuture<Response> send(Request request, Integer partition);

    void stop();
}
