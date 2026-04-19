package com.example;

import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.data.Metric;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicInteger;

public final class ExamplePlugin extends JavaPlugin {
    private final AtomicInteger gameCount = new AtomicInteger();

    private final BukkitMetrics metrics = BukkitMetrics.factory()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("game_count", gameCount::get))
            .addMetric(Metric.string("server_version", () -> "1.0.0"))

            // Error tracking must be enabled in the project settings
            .errorTracker(ErrorTracker.contextAware())

            // #onFlush is invoked after successful metrics submission
            // This is useful for cleaning up cached data
            .onFlush(() -> gameCount.set(0)) // reset game count on flush

            .token("YOUR_TOKEN_HERE") // required -> token can be found in the settings of your project
            .create(this);

    @Override
    public void onEnable() {
        metrics.ready(); // register additional error handlers
    }

    @Override
    public void onDisable() {
        metrics.shutdown(); // safely shut down metrics submission
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
