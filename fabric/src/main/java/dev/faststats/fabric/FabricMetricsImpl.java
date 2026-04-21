package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.Metrics;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

abstract class FabricMetricsImpl extends SimpleMetrics {
    protected final ModContainer mod;

    @Async.Schedule
    @Contract(mutates = "io")
    FabricMetricsImpl(final Factory factory, final ModContainer mod) throws IllegalStateException {
        super(factory);

        this.mod = mod;
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
    }

    protected void appendFabricData(final JsonObject metrics, final String serverType) {
        metrics.addProperty("plugin_version", mod.getMetadata().getVersion().getFriendlyString());
        metrics.addProperty("server_type", serverType);
    }

    static final class Factory extends SimpleMetrics.Factory {
        public Factory(final FabricContext context) {
            super(context);
        }

        @Override
        public Metrics create() throws IllegalStateException, IllegalArgumentException {
            final var mod = ((FabricContext) context).mod;
            return switch (FabricLoader.getInstance().getEnvironmentType()) {
                case CLIENT -> new FabricMetricsClientImpl(this, mod);
                case SERVER -> new FabricMetricsServerImpl(this, mod);
            };
        }
    }
}
