package dev.faststats.hytale;

import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.faststats.config.SimpleConfig;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;

final class HytaleMetricsImpl extends SimpleMetrics implements HytaleMetrics {
    @Async.Schedule
    @Contract(mutates = "io")
    private HytaleMetricsImpl(final Factory factory, final Path config) throws IllegalStateException {
        super(factory, SimpleConfig.read(config));

        startSubmitting();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("server_version", HytaleServer.get().getServerName());
        metrics.addProperty("player_count", Universe.get().getPlayerCount());
        metrics.addProperty("server_type", "Hytale");
    }

    static final class Factory extends SimpleMetrics.Factory<JavaPlugin, HytaleMetrics.Factory> implements HytaleMetrics.Factory {
        @Override
        public Metrics create(final JavaPlugin plugin) throws IllegalStateException {
            final var mods = plugin.getDataDirectory().toAbsolutePath().getParent();
            final var config = mods.resolve("faststats").resolve("config.properties");
            return new HytaleMetricsImpl(this, config);
        }
    }
}
