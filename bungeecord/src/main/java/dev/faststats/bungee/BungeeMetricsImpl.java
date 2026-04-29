package dev.faststats.bungee;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

final class BungeeMetricsImpl extends SimpleMetrics {
    private final ProxyServer server;
    private final Plugin plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    BungeeMetricsImpl(final Factory factory, final Plugin plugin) throws IllegalStateException {
        super(factory);

        this.server = plugin.getProxy();
        this.plugin = plugin;

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) context.getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", server.getConfig().isOnlineMode());
        metrics.addProperty("player_count", server.getOnlineCount());
        metrics.addProperty("plugin_version", plugin.getDescription().getVersion());
        metrics.addProperty("proxy_version", server.getVersion());
        metrics.addProperty("server_type", server.getName());
    }
}
