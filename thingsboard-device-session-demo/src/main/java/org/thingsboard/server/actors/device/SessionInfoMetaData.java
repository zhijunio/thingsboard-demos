package org.thingsboard.server.actors.device;

public final class SessionInfoMetaData {
    private final SessionInfo sessionInfo;
    private long lastActivityTime;
    private boolean subscribedToAttributes;
    private boolean subscribedToRpc;

    public SessionInfoMetaData(SessionInfo sessionInfo, long lastActivityTime) {
        this.sessionInfo = sessionInfo;
        this.lastActivityTime = lastActivityTime;
    }

    public SessionInfo sessionInfo() {
        return sessionInfo;
    }

    public long lastActivityTime() {
        return lastActivityTime;
    }

    public void recordActivity(long timestamp) {
        lastActivityTime = timestamp;
    }

    public boolean subscribedToRpc() {
        return subscribedToRpc;
    }

    public void subscribedToRpc(boolean subscribed) {
        subscribedToRpc = subscribed;
    }

    public boolean subscribedToAttributes() {
        return subscribedToAttributes;
    }

    public void subscribedToAttributes(boolean subscribed) {
        subscribedToAttributes = subscribed;
    }
}
