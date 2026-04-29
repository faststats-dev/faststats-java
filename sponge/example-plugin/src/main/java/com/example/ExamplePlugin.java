package com.example;

import com.google.inject.Inject;
import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.data.Metric;
import dev.faststats.sponge.SpongeContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("example")
public class ExamplePlugin {
    private @Inject SpongeContext.Builder contextBuilder;

    private @Nullable Metrics metrics = null;

    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) {
        final var context = contextBuilder.build("YOUR_TOKEN_HERE");
        this.metrics = context.metrics()
                // Custom metrics require a corresponding data source in your project settings
                .addMetric(Metric.number("example_metric", () -> 42))

                // Error tracking must be enabled in the project settings
                .errorTracker(ErrorTracker.contextAware())

                .create();
    }

    @Listener
    public void onServerStop(final StoppingEngineEvent<Server> event) {
        if (metrics != null) metrics.shutdown(); // safely shut down metrics submission
    }
}
