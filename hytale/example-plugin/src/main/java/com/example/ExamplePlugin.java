package com.example;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import dev.faststats.core.data.Metric;
import dev.faststats.hytale.HytaleMetrics;

public class ExamplePlugin extends JavaPlugin {
    private final Metrics metrics = HytaleMetrics.factory()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("example_metric", () -> 42))

            // Error tracking must be enabled in the project settings
            .errorTracker(ErrorTracker.contextAware())

            .token("YOUR_TOKEN_HERE") // required -> token can be found in the settings of your project
            .create(this);

    public ExamplePlugin(final JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void shutdown() {
        metrics.shutdown(); // safely shut down metrics submission
    }
}
