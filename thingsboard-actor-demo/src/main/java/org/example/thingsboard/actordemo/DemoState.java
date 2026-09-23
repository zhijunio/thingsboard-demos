package org.example.thingsboard.actordemo;

import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class DemoState {
    private final CountDownLatch processedLatch;
    private final CountDownLatch destroyedLatch = new CountDownLatch(1);
    private final List<Integer> processedValues = new CopyOnWriteArrayList<>();
    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger maxConcurrent = new AtomicInteger();
    private final AtomicInteger processFailures = new AtomicInteger();
    private final AtomicReference<TbActorStopReason> destroyReason = new AtomicReference<>();

    public DemoState(int expectedMessages) {
        this.processedLatch = new CountDownLatch(expectedMessages);
    }

    public void onProcessStart(int value) {
        int current = active.incrementAndGet();
        maxConcurrent.accumulateAndGet(current, Math::max);
        processedValues.add(value);
    }

    public void onProcessEnd() {
        active.decrementAndGet();
        processedLatch.countDown();
    }

    public boolean awaitProcessed(long timeout, TimeUnit unit) throws InterruptedException {
        return processedLatch.await(timeout, unit);
    }

    public void onProcessFailure() {
        processFailures.incrementAndGet();
    }

    public void onDestroy(TbActorStopReason reason) {
        destroyReason.set(reason);
        destroyedLatch.countDown();
    }

    public boolean awaitDestroyed(long timeout, TimeUnit unit) throws InterruptedException {
        return destroyedLatch.await(timeout, unit);
    }

    public List<Integer> processedValues() {
        return processedValues;
    }

    public int maxConcurrent() {
        return maxConcurrent.get();
    }

    public int processFailures() {
        return processFailures.get();
    }

    public TbActorStopReason destroyReason() {
        return destroyReason.get();
    }
}
