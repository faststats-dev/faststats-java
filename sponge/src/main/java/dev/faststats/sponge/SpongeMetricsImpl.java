package dev.faststats.sponge;

import com.google.gson.JsonObject;
import dev.faststats.FastStatsContext;
import dev.faststats.Metrics;
import dev.faststats.SimpleMetrics;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

final class SpongeMetricsImpl extends SimpleMetrics implements SpongeMetrics {

    private final PluginContainer plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    private SpongeMetricsImpl(
            final Factory factory,
            final Logger logger,
            final PluginContainer plugin
    ) throws IllegalStateException {
        super(factory);
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

    static class Factory extends SimpleMetrics.Factory {
        public Factory(final FastStatsContext context) {
            super(context);
        }

        @Override
        public Metrics create() throws IllegalStateException, IllegalArgumentException {
            final var context = (SpongeContext) this.context;
            return new SpongeMetricsImpl(this, context.logger, context.plugin);
        }
    }
}
