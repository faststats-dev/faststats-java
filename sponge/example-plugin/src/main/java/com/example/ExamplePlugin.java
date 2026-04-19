package com.example;

import com.google.inject.Inject;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import dev.faststats.core.data.Metric;
import dev.faststats.sponge.SpongeMetrics;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("example")
public class ExamplePlugin {
    private @Inject PluginContainer pluginContainer;
    private @Inject SpongeMetrics.Factory factory;

    private @Nullable Metrics metrics = null;

    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) {
        this.metrics = factory
                // Custom metrics require a corresponding data source in your project settings
                .addMetric(Metric.number("example_metric", () -> 42))

                // Error tracking must be enabled in the project settings
                .errorTracker(ErrorTracker.contextAware())

                .token("YOUR_TOKEN_HERE") // required -> token can be found in the settings of your project
                .create(pluginContainer);
    }

    @Listener
    public void onServerStop(final StoppingEngineEvent<Server> event) {
        if (metrics != null) metrics.shutdown(); // safely shut down metrics submission
    }
}
