package org.thingsboard.server.actors;

public final class InitFailureStrategy {
    private final boolean stop;
    private final long retryDelay;

    private InitFailureStrategy(boolean stop, long retryDelay) {
        this.stop = stop;
        this.retryDelay = retryDelay;
    }

    public static InitFailureStrategy retryImmediately() {
        return new InitFailureStrategy(false, 0);
    }

    public static InitFailureStrategy retryWithDelay(long milliseconds) {
        return new InitFailureStrategy(false, milliseconds);
    }

    public static InitFailureStrategy stop() {
        return new InitFailureStrategy(true, 0);
    }

    public boolean isStop() {
        return stop;
    }

    public long getRetryDelay() {
        return retryDelay;
    }
}
