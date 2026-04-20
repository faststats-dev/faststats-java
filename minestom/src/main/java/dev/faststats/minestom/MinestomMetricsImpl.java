package dev.faststats.minestom;

import com.google.gson.JsonObject;
import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

final class MinestomMetricsImpl extends SimpleMetrics implements MinestomMetrics {
    @Async.Schedule
    @Contract(mutates = "io")
    private MinestomMetricsImpl(final Factory factory) throws IllegalStateException {
        super(factory);

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
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

    static final class Factory extends SimpleMetrics.Factory<MinestomMetrics.Factory> implements MinestomMetrics.Factory {
        Factory(final MinestomContext context) {
            super(context);
        }

        @Override
        public Metrics create() throws IllegalStateException {
            return new MinestomMetricsImpl(this);
        }
    }
}
