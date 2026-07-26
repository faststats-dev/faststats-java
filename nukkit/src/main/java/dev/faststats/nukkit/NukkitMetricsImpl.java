package dev.faststats.nukkit;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;

import java.util.Optional;
import java.util.function.Supplier;

final class NukkitMetricsImpl extends SimpleMetrics {
    private final Server server;
    private final PluginBase plugin;

    public NukkitMetricsImpl(final Factory factory, final PluginBase plugin) throws IllegalStateException {
        super(factory);

        this.server = plugin.getServer();
        this.plugin = plugin;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("game_version", server.getVersion());
        metrics.addProperty("online_mode", server.xboxAuth);
        metrics.addProperty("platform_version", server.getNukkitVersion());
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
}
