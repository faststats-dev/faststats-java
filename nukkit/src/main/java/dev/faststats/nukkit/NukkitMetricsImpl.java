package dev.faststats.nukkit;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.google.gson.JsonObject;
import dev.faststats.Metrics;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.function.Supplier;

final class NukkitMetricsImpl extends SimpleMetrics implements NukkitMetrics {
    private final Server server;
    private final PluginBase plugin;

    @Async.Schedule
    @Contract(mutates = "io")
    private NukkitMetricsImpl(final Factory factory, final PluginBase plugin) throws IllegalStateException {
        super(factory);

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

    static final class Factory extends SimpleMetrics.Factory<NukkitMetrics.Factory> implements NukkitMetrics.Factory {
        Factory(final NukkitContext context) {
            super(context);
        }

        @Override
        public Metrics create() throws IllegalStateException {
            return new NukkitMetricsImpl(this, ((NukkitContext) context).plugin);
        }
    }
}
