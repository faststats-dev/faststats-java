package dev.faststats.nukkit;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.google.gson.JsonObject;
import dev.faststats.config.SimpleConfig;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

final class NukkitMetricsImpl extends SimpleMetrics implements NukkitMetrics {
    private final Server server;
    private final PluginBase plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    private NukkitMetricsImpl(final Factory factory, final PluginBase plugin, final Path config) throws IllegalStateException {
        super(factory, SimpleConfig.read(config));

        this.server = plugin.getServer();
        this.plugin = plugin;

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("minecraft_version", server.getVersion());
        metrics.addProperty("online_mode", server.xboxAuth);
        metrics.addProperty("player_count", server.getOnlinePlayersCount());
        metrics.addProperty("plugin_version", plugin.getDescription().getVersion());
        metrics.addProperty("server_type", server.getName());
    }

    private <T> Optional<T> tryOrEmpty(final Supplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (final NoSuchMethodError | Exception e) {
            return Optional.empty();
        }
    }

    static final class Factory extends SimpleMetrics.Factory<PluginBase, NukkitMetrics.Factory> implements NukkitMetrics.Factory {
        @Override
        public Metrics create(final PluginBase plugin) throws IllegalStateException {
            final var dataFolder = Path.of(plugin.getServer().getPluginPath(), "faststats");
            final var config = dataFolder.resolve("config.properties");
            return new NukkitMetricsImpl(this, plugin, config);
        }
    }
}
