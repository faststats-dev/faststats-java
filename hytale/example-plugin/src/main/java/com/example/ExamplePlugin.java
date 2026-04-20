package com.example;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.data.Metric;
import dev.faststats.hytale.HytaleContext;

public class ExamplePlugin extends JavaPlugin {
    private final HytaleContext context = new HytaleContext(this, "YOUR_TOKEN_HERE");
    private final Metrics metrics = context.metrics()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("example_metric", () -> 42))

            // Error tracking must be enabled in the project settings
            .errorTracker(ErrorTracker.contextAware())

            .create(this);

    public ExamplePlugin(final JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void shutdown() {
        metrics.shutdown(); // safely shut down metrics submission
    }
}
