package dev.faststats.event;

import org.jetbrains.annotations.Contract;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public sealed interface EventSink permits SimpleEventSink {
    Duration DEFAULT_DRAIN_INTERVAL = Duration.ofMinutes(30);

    @Contract(pure = true)
    Duration drainInterval();

    @Contract(value = "_ -> this", mutates = "this")
    EventSink drainInterval(Duration duration);

    @Contract(mutates = "this")
    void queueHeartbeat(Event event);

    @Contract(mutates = "this")
    void queueEvent(Event event);

    @Contract(mutates = "io")
    CompletableFuture<Void> publish(Event event);

    @Contract(mutates = "io")
    CompletableFuture<Void> drain();
}
