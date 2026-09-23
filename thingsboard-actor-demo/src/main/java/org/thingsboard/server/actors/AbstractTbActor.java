package org.thingsboard.server.actors;

public abstract class AbstractTbActor implements TbActor {
    protected TbActorCtx ctx;

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        this.ctx = ctx;
    }

    @Override
    public TbActorRef getActorRef() {
        return ctx;
    }
}
