package dev.faststats.minestom;

import com.google.gson.JsonObject;
import dev.faststats.config.SimpleConfig;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;

final class MinestomMetricsImpl extends SimpleMetrics implements MinestomMetrics {
    @Async.Schedule
    @Contract(mutates = "io")
    private MinestomMetricsImpl(final Factory factory, final Path config) throws IllegalStateException {
        super(factory, SimpleConfig.read(config));

        startSubmitting();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("minecraft_version", MinecraftServer.VERSION_NAME);
        metrics.addProperty("online_mode", !(MinecraftServer.process().auth() instanceof Auth.Offline));
        metrics.addProperty("player_count", MinecraftServer.getConnectionManager().getOnlinePlayerCount());
        metrics.addProperty("server_type", "Minestom");
    }

    @Override
    public void ready() {
        getErrorTracker().ifPresent(this::registerExceptionHandler);
    }

    private void registerExceptionHandler(final ErrorTracker errorTracker) {
        final var handler = MinecraftServer.getExceptionManager().getExceptionHandler();
        MinecraftServer.getExceptionManager().setExceptionHandler(error -> {
            handler.handleException(error);
            if (!ErrorTracker.isSameLoader(getClass().getClassLoader(), error)) return;
            errorTracker.trackError(error);
        });
    }

    static final class Factory extends SimpleMetrics.Factory<MinecraftServer, MinestomMetrics.Factory> implements MinestomMetrics.Factory {
        @Override
        public Metrics create(final MinecraftServer server) throws IllegalStateException {
            final var config = Path.of("faststats", "config.properties");
            return new MinestomMetricsImpl(this, config);
        }
    }
}
