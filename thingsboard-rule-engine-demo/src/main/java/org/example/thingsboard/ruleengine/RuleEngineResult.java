package org.example.thingsboard.ruleengine;

import org.thingsboard.server.common.msg.TbMsgCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class RuleEngineResult implements TbMsgCallback {
    private final CompletableFuture<Result> future = new CompletableFuture<>();

    @Override
    public void onSuccess() {
        future.complete(new Result(true, ""));
    }

    @Override
    public void onFailure(String reason) {
        future.complete(new Result(false, reason));
    }

    public Result await() {
        return future.join();
    }

    public Result await(long timeout, TimeUnit unit) throws Exception {
        return future.get(timeout, unit);
    }

    public record Result(boolean success, String reason) {
    }
}
