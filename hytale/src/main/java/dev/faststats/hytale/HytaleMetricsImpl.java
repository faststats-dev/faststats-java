package dev.faststats.hytale;

import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.faststats.Config;
import dev.faststats.Metrics;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

final class HytaleMetricsImpl extends SimpleMetrics {
    @Async.Schedule
    @Contract(mutates = "io")
    private HytaleMetricsImpl(final Factory factory) throws IllegalStateException {
        super(factory);

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("server_version", HytaleServer.get().getServerName());
        metrics.addProperty("player_count", Universe.get().getPlayerCount());
        metrics.addProperty("server_type", "Hytale");
    }

    static final class Factory extends SimpleMetrics.Factory {
        public Factory(final HytaleContext context) {
            super(context);
        }

        @Override
        public Metrics create() throws IllegalStateException {
            return new HytaleMetricsImpl(this);
        }
    }
}
