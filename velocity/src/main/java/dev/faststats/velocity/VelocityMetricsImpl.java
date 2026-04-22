package dev.faststats.velocity;

import com.google.gson.JsonObject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

final class VelocityMetricsImpl extends SimpleMetrics {
    private final ProxyServer server;
    private final PluginContainer plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    VelocityMetricsImpl(final Factory factory) throws IllegalStateException {
        super(factory);

        final var context = (VelocityContext) this.context;
        this.server = context.server;
        this.plugin = context.plugin;

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) context.getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        final var pluginVersion = plugin.getDescription().getVersion().orElse("unknown");
        metrics.addProperty("online_mode", server.getConfiguration().isOnlineMode());
        metrics.addProperty("player_count", server.getPlayerCount());
        metrics.addProperty("plugin_version", pluginVersion);
        metrics.addProperty("proxy_version", server.getVersion().getVersion());
        metrics.addProperty("server_type", server.getVersion().getName());
    }
}
