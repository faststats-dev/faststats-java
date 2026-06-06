package com.example;

import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.quilt.QuiltContext;
import net.fabricmc.api.ModInitializer;

import java.util.concurrent.atomic.AtomicInteger;

public final class ExampleMod implements ModInitializer {
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    private final AtomicInteger gameCount = new AtomicInteger();

    private final QuiltContext context = new QuiltContext.Factory(
            "example_mod",
            "YOUR_TOKEN_HERE"
    )
            .metrics(factory -> factory
                    .addMetric(Metric.number("game_count", gameCount::get))
                    .addMetric(Metric.string("server_version", () -> "1.0.0"))
                    .onFlush(() -> gameCount.set(0))
                    .create())
            .errorTrackerService(ERROR_TRACKER)
            .create();

    @Override
    public void onInitialize() {
        // your actual logic
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
