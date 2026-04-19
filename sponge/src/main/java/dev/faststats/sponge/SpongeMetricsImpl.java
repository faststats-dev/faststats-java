package dev.faststats.sponge;

import com.google.gson.JsonObject;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;

final class SpongeMetricsImpl extends SimpleMetrics implements SpongeMetrics {
    public static final String COMMENT = """
             FastStats (https://faststats.dev) collects anonymous usage statistics for plugin developers.
            # This helps developers understand how their projects are used in the real world.
            #
            # No IP addresses, player data, or personal information is collected.
            # The server ID below is randomly generated and can be regenerated at any time.
            #
            # Enabling metrics has no noticeable performance impact.
            # Enabling metrics is recommended, you can do so in the Sponge metrics.config,
            # by setting the "global-state" property to "TRUE".
            #
            # If you suspect a plugin is collecting personal data or bypassing the Sponge config,
            # please report it at: https://faststats.dev/abuse
            #
            # For more information, visit: https://faststats.dev/info
            """;

    private final PluginContainer plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    private SpongeMetricsImpl(
            final Factory factory,
            final Logger logger,
            final PluginContainer plugin,
            final Path config
    ) throws IllegalStateException {
        super(factory, SimpleMetrics.Config.read(config, COMMENT, true, Sponge.metricsConfigManager()
                .effectiveCollectionState(plugin).asBoolean()));
        this.plugin = plugin;
        startSubmitting();
    }

    @Override
    protected String getOnboardingMessage() {
        return """
                This plugin uses FastStats to collect anonymous usage statistics.
                No personal or identifying information is ever collected.
                It is recommended to enable metrics by setting 'global-state=TRUE' in the sponge metrics config.
                Learn more at: https://faststats.dev/info
                """;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", Sponge.server().isOnlineModeEnabled());
        metrics.addProperty("player_count", Sponge.server().onlinePlayers().size());
        metrics.addProperty("plugin_version", plugin.metadata().version().toString());
        metrics.addProperty("minecraft_version", Sponge.platform().minecraftVersion().name());
        metrics.addProperty("server_type", Sponge.platform().container(Platform.Component.IMPLEMENTATION).metadata().id());
    }

    static class Factory extends SimpleMetrics.Factory<PluginContainer, SpongeMetrics.Factory> {
        protected final Logger logger;
        protected final Path dataDirectory;

        public Factory(final Logger logger, final Path dataDirectory) {
            this.logger = logger;
            this.dataDirectory = dataDirectory;
        }

        @Override
        public Metrics create(final PluginContainer plugin) throws IllegalStateException, IllegalArgumentException {
            final var faststats = dataDirectory.resolve("faststats");
            return new SpongeMetricsImpl(this, logger, plugin, faststats.resolve("config.properties"));
        }
    }
}
