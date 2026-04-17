package com.example;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import dev.faststats.core.Settings;
import dev.faststats.core.data.Metric;
import dev.faststats.velocity.VelocityMetrics;
import org.jspecify.annotations.Nullable;

@Plugin(id = "example", name = "Example Plugin", version = "1.0.0",
        url = "https://example.com", authors = {"Your Name"})
public class ExamplePlugin {
    private final VelocityMetrics.Factory metricsFactory;
    private @Nullable Metrics metrics = null;

    @Inject
    public ExamplePlugin(final VelocityMetrics.Factory factory) {
        this.metricsFactory = factory;
    }

    @Subscribe
    public void onProxyInitialize(final ProxyInitializeEvent event) {
        this.metrics = metricsFactory
                // Custom metrics require a corresponding data source in your project settings
                .addMetric(Metric.number("example_metric", () -> 42))

                // Error tracking must be enabled in the project settings
                .errorTracker(ErrorTracker.contextAware())

                .settings(Settings.withToken("YOUR_TOKEN_HERE")) // token can be found in the settings of your project
                .create(this);
    }

    @Subscribe
    public void onProxyStop(final ProxyShutdownEvent event) {
        if (metrics != null) metrics.shutdown(); // safely shut down metrics submission
    }
}
