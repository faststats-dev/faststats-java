package com.example;

import dev.faststats.bungee.BungeeMetrics;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import dev.faststats.core.Settings;
import dev.faststats.core.data.Metric;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;

public class ExamplePlugin extends Plugin {
    private final AtomicInteger gameCount = new AtomicInteger();

    private final Metrics metrics = BungeeMetrics.factory()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("game_count", gameCount::get))
            .addMetric(Metric.string("server_version", () -> "1.0.0"))

            // Error tracking must be enabled in the project settings
            .errorTracker(ErrorTracker.contextAware())

            // #onFlush is invoked after successful metrics submission
            // This is useful for cleaning up cached data
            .onFlush(() -> gameCount.set(0)) // reset game count on flush

            .settings(Settings.withToken("YOUR_TOKEN_HERE")) // token can be found in the settings of your project
            .create(this);

    @Override
    public void onDisable() {
        metrics.shutdown(); // safely shut down metrics submission
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
