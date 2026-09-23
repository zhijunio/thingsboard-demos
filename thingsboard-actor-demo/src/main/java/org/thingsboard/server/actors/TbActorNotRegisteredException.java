package org.thingsboard.server.actors;

public class TbActorNotRegisteredException extends RuntimeException {
    private final TbActorId target;

    public TbActorNotRegisteredException(TbActorId target, String message) {
        super(message);
        this.target = target;
    }

    public TbActorId getTarget() {
        return target;
    }
}
