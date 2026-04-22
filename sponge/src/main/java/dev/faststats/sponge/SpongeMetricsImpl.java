package dev.faststats.sponge;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

final class SpongeMetricsImpl extends SimpleMetrics {
    private final PluginContainer plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    SpongeMetricsImpl(final Factory factory) throws IllegalStateException {
        super(factory);
        this.plugin = ((SpongeContext) this.context).plugin;
        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SpongeConfig) context.getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", Sponge.server().isOnlineModeEnabled());
        metrics.addProperty("player_count", Sponge.server().onlinePlayers().size());
        metrics.addProperty("plugin_version", plugin.metadata().version().toString());
        metrics.addProperty("minecraft_version", Sponge.platform().minecraftVersion().name());
        metrics.addProperty("server_type", Sponge.platform().container(Platform.Component.IMPLEMENTATION).metadata().id());
    }
}
