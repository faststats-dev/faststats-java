package dev.faststats.hytale;

import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.logging.Level;

final class HytaleMetricsImpl extends SimpleMetrics implements HytaleMetrics {
    private final HytaleLogger logger;

    @Async.Schedule
    @Contract(mutates = "io")
    private HytaleMetricsImpl(final Factory factory, final HytaleLogger logger, final Path config) throws IllegalStateException {
        super(factory, config);
        this.logger = logger;

        startSubmitting();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("server_version", HytaleServer.get().getServerName());
        metrics.addProperty("player_count", Universe.get().getPlayerCount());
        metrics.addProperty("server_type", "Hytale");
    }

    @Override
    protected void error(final String message, @Nullable final Throwable throwable, @Nullable final Object... args) {
        if (super.logger.isLoggable(Level.SEVERE)) logger.atSevere().withCause(throwable).logVarargs(message, args);
    }

    @Override
    protected void log(final Level level, final String message, @Nullable final Object... args) {
        if (super.logger.isLoggable(level)) logger.at(level).logVarargs(message, args);
    }

    static final class Factory extends SimpleMetrics.Factory<JavaPlugin, HytaleMetrics.Factory> implements HytaleMetrics.Factory {
        @Override
        public Metrics create(final JavaPlugin plugin) throws IllegalStateException {
            final var mods = plugin.getDataDirectory().toAbsolutePath().getParent();
            final var config = mods.resolve("faststats").resolve("config.properties");
            return new HytaleMetricsImpl(this, plugin.getLogger(), config);
        }
    }
}
