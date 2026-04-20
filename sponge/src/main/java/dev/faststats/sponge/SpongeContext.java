package dev.faststats.sponge;

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
            @Token final String token // fixme: cannot have a token here
    ) {
        super(SpongeConfig.read(plugin, dataDirectory.resolve("faststats").resolve("config.properties")), token);
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public SpongeMetrics.Factory metrics() {
        return new SpongeMetrics.Factory(this);
    }
}
