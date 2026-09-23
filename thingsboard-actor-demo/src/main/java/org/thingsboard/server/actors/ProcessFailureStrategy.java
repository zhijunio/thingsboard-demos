package org.thingsboard.server.actors;

public final class ProcessFailureStrategy {
    private final boolean stop;

    private ProcessFailureStrategy(boolean stop) {
        this.stop = stop;
    }

    public static ProcessFailureStrategy stop() {
        return new ProcessFailureStrategy(true);
    }

    public static ProcessFailureStrategy resume() {
        return new ProcessFailureStrategy(false);
    }

    public boolean isStop() {
        return stop;
    }
}
