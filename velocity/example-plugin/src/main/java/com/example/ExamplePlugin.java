package com.example;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.data.Metric;
import dev.faststats.velocity.VelocityContext;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "example", name = "Example Plugin", version = "1.0.0",
        url = "https://example.com", authors = {"Your Name"})
public class ExamplePlugin {
    private final VelocityContext context;
    private @Nullable Metrics metrics = null;

    @Inject
    public ExamplePlugin(final ProxyServer server, final Logger logger, @DataDirectory final Path dataDirectory) {
        this.context = new VelocityContext(server, logger, dataDirectory, "YOUR_TOKEN_HERE");
    }

    @Subscribe
    public void onProxyInitialize(final ProxyInitializeEvent event) {
        this.metrics = context.metrics()
                // Custom metrics require a corresponding data source in your project settings
                .addMetric(Metric.number("example_metric", () -> 42))

                // Error tracking must be enabled in the project settings
                .errorTracker(ErrorTracker.contextAware())

                .create(this);
    }

    @Subscribe
    public void onProxyStop(final ProxyShutdownEvent event) {
        if (metrics != null) metrics.shutdown(); // safely shut down metrics submission
    }
}
