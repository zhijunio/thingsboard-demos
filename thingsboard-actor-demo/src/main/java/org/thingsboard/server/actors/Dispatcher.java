package org.thingsboard.server.actors;

import java.util.concurrent.ExecutorService;

record Dispatcher(String id, ExecutorService executor) {
}
