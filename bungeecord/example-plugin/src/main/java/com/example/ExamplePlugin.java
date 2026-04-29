package com.example;

import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.bungee.BungeeContext;
import dev.faststats.data.Metric;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;

public class ExamplePlugin extends Plugin {
    private final AtomicInteger gameCount = new AtomicInteger();
    private final BungeeContext context = new BungeeContext(this, "YOUR_TOKEN_HERE");

    private final Metrics metrics = context.metrics()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("game_count", gameCount::get))
            .addMetric(Metric.string("server_version", () -> "1.0.0"))

            // Error tracking must be enabled in the project settings
            .errorTracker(ErrorTracker.contextAware())

            // #onFlush is invoked after successful metrics submission
            // This is useful for cleaning up cached data
            .onFlush(() -> gameCount.set(0)) // reset game count on flush

            .create();

    @Override
    public void onDisable() {
        metrics.shutdown(); // safely shut down metrics submission
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
