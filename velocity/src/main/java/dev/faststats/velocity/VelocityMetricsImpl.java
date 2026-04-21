package dev.faststats.velocity;

import com.google.gson.JsonObject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.Config;
import dev.faststats.FastStatsContext;
import dev.faststats.Metrics;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import java.nio.file.Path;

final class VelocityMetricsImpl extends SimpleMetrics implements VelocityMetrics {
    private final ProxyServer server;
    private final PluginContainer plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    private VelocityMetricsImpl(
            final Factory factory,
            final Logger logger,
            final ProxyServer server,
            final Config config,
            final PluginContainer plugin
    ) throws IllegalStateException {
        super(factory, config);

        this.server = server;
        this.plugin = plugin;

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        final var pluginVersion = plugin.getDescription().getVersion().orElse("unknown");
        metrics.addProperty("online_mode", server.getConfiguration().isOnlineMode());
        metrics.addProperty("player_count", server.getPlayerCount());
        metrics.addProperty("plugin_version", pluginVersion);
        metrics.addProperty("proxy_version", server.getVersion().getVersion());
        metrics.addProperty("server_type", server.getVersion().getName());
    }

    static class Factory extends SimpleMetrics.Factory {
        protected final Logger logger;
        protected final Path dataDirectory;
        protected final ProxyServer server;
        protected final PluginContainer plugin;

        public Factory(
                final FastStatsContext context,
                final PluginContainer plugin,
                final ProxyServer server,
                final Logger logger,
                @DataDirectory final Path dataDirectory
        ) {
            super(context);
            this.plugin = plugin;
            this.logger = logger;
            this.dataDirectory = dataDirectory;
            this.server = server;
        }

        /**
         * Creates a new metrics instance.
         * <p>
         * Metrics submission will start automatically.
         *
         * @return the metrics instance
         * @throws IllegalStateException    if the token is not specified
         * @since 0.23.0
         */
        @Override
        public Metrics create() throws IllegalStateException {
            return new VelocityMetricsImpl(this, logger, server, context.getConfig(), plugin);
        }
    }
}
