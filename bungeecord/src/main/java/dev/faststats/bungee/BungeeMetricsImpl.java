package dev.faststats.bungee;

import com.google.gson.JsonObject;
import dev.faststats.config.SimpleConfig;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;

final class BungeeMetricsImpl extends SimpleMetrics implements BungeeMetrics {
    private final ProxyServer server;
    private final Plugin plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    private BungeeMetricsImpl(final Factory factory, final Plugin plugin, final Path config) throws IllegalStateException {
        super(factory, SimpleConfig.read(config));

        this.server = plugin.getProxy();
        this.plugin = plugin;

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", server.getConfig().isOnlineMode());
        metrics.addProperty("player_count", server.getOnlineCount());
        metrics.addProperty("plugin_version", plugin.getDescription().getVersion());
        metrics.addProperty("proxy_version", server.getVersion());
        metrics.addProperty("server_type", server.getName());
    }

    static final class Factory extends SimpleMetrics.Factory<Plugin, BungeeMetrics.Factory> implements BungeeMetrics.Factory {
        @Override
        public Metrics create(final Plugin plugin) throws IllegalStateException {
            final var dataFolder = plugin.getProxy().getPluginsFolder().toPath().resolve("faststats");
            final var config = dataFolder.resolve("config.properties");
            return new BungeeMetricsImpl(this, plugin, config);
        }
    }
}
