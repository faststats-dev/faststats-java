package dev.faststats.sponge;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

final class SpongeMetricsImpl extends SimpleMetrics {
    private final PluginContainer plugin;

    SpongeMetricsImpl(final Factory factory) throws IllegalStateException {
        super(factory);
        this.plugin = ((SpongeContext) this.context).plugin;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        final var implementation = Sponge.platform().container(Platform.Component.IMPLEMENTATION);
        metrics.addProperty("game_version", Sponge.platform().minecraftVersion().name());
        metrics.addProperty("online_mode", Sponge.server().isOnlineModeEnabled());
        metrics.addProperty("platform_version", implementation.metadata().version().toString()); // todo: double check
        metrics.addProperty("player_count", Sponge.server().onlinePlayers().size());
        metrics.addProperty("plugin_version", plugin.metadata().version().toString());
        metrics.addProperty("server_type", implementation.metadata().id()); // todo: double check
    }
}
