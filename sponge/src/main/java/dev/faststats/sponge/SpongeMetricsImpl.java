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

    private final PluginContainer plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    private SpongeMetricsImpl(
            final Factory factory,
            final Logger logger,
            final PluginContainer plugin,
            final Path config
    ) throws IllegalStateException {
        super(factory, SpongeConfig.read(plugin, config));
        this.plugin = plugin;
        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SpongeConfig) getConfig()).preSubmissionStart();
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
