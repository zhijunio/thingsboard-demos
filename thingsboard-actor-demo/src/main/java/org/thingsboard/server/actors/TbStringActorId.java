package org.thingsboard.server.actors;

import org.thingsboard.server.common.data.EntityType;

import java.util.Objects;

public final class TbStringActorId implements TbActorId {
    private final String id;

    public TbStringActorId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEMO;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TbStringActorId that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
