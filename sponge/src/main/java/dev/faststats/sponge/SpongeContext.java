package dev.faststats.sponge;

import com.google.inject.Inject;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;

/**
 * Sponge FastStats context.
 *
 * @since 0.23.0
 */
public final class SpongeContext extends SimpleContext {
    final PluginContainer plugin;
    final Logger logger;

    public SpongeContext(
            final PluginContainer plugin,
            final Logger logger,
            @ConfigDir(sharedRoot = true) final Path dataDirectory,
            @Token final String token
    ) {
        super(SpongeConfig.read(plugin, dataDirectory.resolve("faststats").resolve("config.properties")), token);
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public SpongeMetrics.Factory metrics() {
        return new SpongeMetrics.Factory(this);
    }

    /**
     * Injectable Sponge context builder.
     *
     * @since 0.23.0
     */
    public static final class Builder {
        private final PluginContainer plugin;
        private final Logger logger;
        private final Path dataDirectory;

        /**
         * Creates a new Sponge context builder.
         *
         * @param plugin        the plugin container
         * @param logger        the plugin logger
         * @param dataDirectory the shared Sponge config directory
         * @apiNote This instance can be injected into your plugin.
         * @since 0.23.0
         */
        @Inject
        public Builder(
                final PluginContainer plugin,
                final Logger logger,
                @ConfigDir(sharedRoot = true) final Path dataDirectory
        ) {
            this.plugin = plugin;
            this.logger = logger;
            this.dataDirectory = dataDirectory;
        }

        /**
         * Builds the finalized Sponge context.
         *
         * @param token the FastStats project token
         * @return the Sponge context
         * @throws IllegalArgumentException if the token is invalid
         * @since 0.23.0
         */
        public SpongeContext build(@Token final String token) throws IllegalArgumentException {
            return new SpongeContext(plugin, logger, dataDirectory, token);
        }
    }
}
