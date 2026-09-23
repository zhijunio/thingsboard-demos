package org.thingsboard.server.actors;

public record TbActorSystemSettings(int actorThroughput, int schedulerPoolSize, int maxActorInitAttempts) {
}
