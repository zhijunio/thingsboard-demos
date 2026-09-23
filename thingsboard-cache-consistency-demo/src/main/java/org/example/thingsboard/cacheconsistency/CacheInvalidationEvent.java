package org.example.thingsboard.cacheconsistency;

public record CacheInvalidationEvent(String key, long version) {
}
