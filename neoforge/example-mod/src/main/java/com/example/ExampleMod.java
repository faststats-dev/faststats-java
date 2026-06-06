package com.example;

import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.neoforge.NeoForgeContext;
import net.neoforged.fml.common.Mod;

import java.util.concurrent.atomic.AtomicInteger;

@Mod("example_mod")
public final class ExampleMod {
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    private final AtomicInteger gameCount = new AtomicInteger();

    private final NeoForgeContext context = new NeoForgeContext.Factory(
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

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
