package dev.faststats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;

import java.nio.file.Path;

/**
 * Velocity FastStats context.
 *
 * @since 0.23.0
 */
public final class VelocityContext extends SimpleContext {
    final PluginContainer plugin;
    final ProxyServer server;

    private VelocityContext(
            final PluginContainer plugin,
            final ProxyServer server,
            @DataDirectory final Path dataDirectory,
            @Token final String token
    ) {
        super(SimpleConfig.read(dataDirectory.resolveSibling("faststats").resolve("config.properties")), token);
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public Metrics.Factory metrics() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new VelocityMetricsImpl(this);
            }
        };
    }

    /**
     * Injectable Velocity context builder.
     *
     * @since 0.23.0
     */
    public static final class Builder {
        private final PluginContainer plugin;
        private final ProxyServer server;
        private final Path dataDirectory;

        /**
         * Creates a new Velocity context builder.
         *
         * @param server        the velocity server
         * @param dataDirectory the plugin data directory
         * @apiNote This instance can be injected into your plugin.
         * @since 0.23.0
         */
        @Inject
        public Builder(
                final PluginContainer plugin,
                final ProxyServer server,
                @DataDirectory final Path dataDirectory
        ) {
            this.plugin = plugin;
            this.server = server;
            this.dataDirectory = dataDirectory;
        }

        /**
         * Builds the finalized Velocity context.
         *
         * @param token the FastStats project token
         * @return the Velocity context
         * @throws IllegalArgumentException if the token is invalid
         * @since 0.23.0
         */
        public VelocityContext build(@Token final String token) throws IllegalArgumentException {
            return new VelocityContext(plugin, server, dataDirectory, token);
        }
    }
}
