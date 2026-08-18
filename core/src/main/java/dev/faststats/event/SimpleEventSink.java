package dev.faststats.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SimpleEventSink implements EventSink {
    private volatile Duration drainInterval = DEFAULT_DRAIN_INTERVAL;
    private final Set<Event> heartbeats = ConcurrentHashMap.newKeySet();
    private final Set<Event> events = ConcurrentHashMap.newKeySet();

    @Override
    public Duration drainInterval() {
        return drainInterval;
    }

    @Override
    public EventSink drainInterval(final Duration duration) {
        this.drainInterval = duration;
        return this;
    }

    @Override
    public void queueHeartbeat(final Event event) {
        this.heartbeats.add(event);
    }

    @Override
    public void queueEvent(final Event event) {
        this.events.add(event);
    }

    public JsonObject compileAll() {
        final var root = new JsonObject();
        root.addProperty("token", "insert token here");
        root.addProperty("debug", false);
        final var events = new JsonArray();
        final var heartbeat = new JsonObject();
        heartbeat.addProperty("event", "heartbeat");
        mapEvents(heartbeats).forEach(heartbeat::add);
        root.add("events", events);
        return root;
    }

    private Map<String, JsonElement> mapEvents(final Set<Event> events) {
        final var map = new HashMap<String, JsonElement>();
        events.forEach(event -> {
            final var object = new JsonObject();
            object.add(event.key().value(), event.payload());
            map.put(event.key().namespace(), object);
        });
        return map;
    }

    public JsonObject compile(final Event event) {
        return new JsonObject();
    }

    @Override
    public CompletableFuture<Void> publish(final Event event) {
        return null;
    }

    @Override
    public CompletableFuture<Void> drain() {
        return null;
    }
}
