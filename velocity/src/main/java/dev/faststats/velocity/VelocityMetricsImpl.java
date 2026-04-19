package dev.faststats.velocity;

import com.google.gson.JsonObject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
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
            final Path config,
            final PluginContainer plugin
    ) throws IllegalStateException {
        super(factory, config);

        this.server = server;
        this.plugin = plugin;

        startSubmitting();
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

    static class Factory extends SimpleMetrics.Factory<Object, VelocityMetrics.Factory> {
        protected final Logger logger;
        protected final Path dataDirectory;
        protected final ProxyServer server;

        public Factory(final ProxyServer server, final Logger logger, @DataDirectory final Path dataDirectory) {
            this.logger = logger;
            this.dataDirectory = dataDirectory;
            this.server = server;
        }

        /**
         * Creates a new metrics instance.
         * <p>
         * Metrics submission will start automatically.
         *
         * @param plugin the plugin instance
         * @return the metrics instance
         * @throws IllegalStateException    if the token is not specified
         * @throws IllegalArgumentException if the given object is not a valid plugin
         * @see #token(String)
         * @since 0.1.0
         */
        @Override
        public Metrics create(final Object plugin) throws IllegalStateException, IllegalArgumentException {
            final var faststats = dataDirectory.resolveSibling("faststats");
            final var container = server.getPluginManager().ensurePluginContainer(plugin);
            return new VelocityMetricsImpl(this, logger, server, faststats.resolve("config.properties"), container);
        }
    }
}
