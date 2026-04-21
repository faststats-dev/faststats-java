package dev.faststats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.Config;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity FastStats context.
 *
 * @since 0.23.0
 */
public final class VelocityContext extends SimpleContext {
    private final PluginContainer plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public VelocityContext(
            final Config config,
            final PluginContainer plugin,
            final ProxyServer server,
            final Logger logger,
            final Path dataDirectory,
            @Token final String token
    ) {
        super(config, token);
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public VelocityContext(
            final PluginContainer plugin,
            final ProxyServer server,
            final Logger logger,
            @DataDirectory final Path dataDirectory,
            @Token final String token
    ) {
        this(SimpleConfig.read(dataDirectory.resolveSibling("faststats").resolve("config.properties")), plugin, server, logger, dataDirectory, token);
    }

    @Override
    public VelocityMetrics.Factory metrics() {
        return new VelocityMetrics.Factory(this, plugin, server, logger, dataDirectory);
    }

    /**
     * Injectable Velocity context builder.
     *
     * @since 0.23.0
     */
    public static final class Builder {
        private final PluginContainer plugin;
        private final ProxyServer server;
        private final Logger logger;
        private final Path dataDirectory;

        /**
         * Creates a new Velocity context builder.
         *
         * @param server        the velocity server
         * @param logger        the plugin logger
         * @param dataDirectory the plugin data directory
         * @apiNote This instance can be injected into your plugin.
         * @since 0.23.0
         */
        @Inject
        public Builder(
                final PluginContainer plugin,
                final ProxyServer server,
                final Logger logger,
                @DataDirectory final Path dataDirectory
        ) {
            this.plugin = plugin;
            this.server = server;
            this.logger = logger;
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
            return new VelocityContext(plugin, server, logger, dataDirectory, token);
        }
    }
}
