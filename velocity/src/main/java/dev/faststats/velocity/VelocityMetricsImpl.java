package dev.faststats.velocity;

import com.google.gson.JsonObject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.SimpleMetrics;

final class VelocityMetricsImpl extends SimpleMetrics {
    private final ProxyServer server;
    private final PluginContainer plugin;

    VelocityMetricsImpl(final Factory factory) throws IllegalStateException {
        super(factory);

        final var context = (VelocityContext) this.context;
        this.server = context.server;
        this.plugin = context.plugin;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", server.getConfiguration().isOnlineMode());
        metrics.addProperty("platform_version", server.getVersion().getVersion());
        metrics.addProperty("player_count", server.getPlayerCount());
        plugin.getDescription().getVersion().ifPresent(s -> metrics.addProperty("plugin_version", s));
        metrics.addProperty("server_type", server.getVersion().getName());
    }
}
