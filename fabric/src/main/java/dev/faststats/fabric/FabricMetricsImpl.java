package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
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
        return ((SimpleConfig) context.getConfig()).preSubmissionStart();
    }

    protected void appendFabricData(final JsonObject metrics, final String serverType) {
        metrics.addProperty("plugin_version", mod.getMetadata().getVersion().getFriendlyString());
        metrics.addProperty("server_type", serverType);
    }
}
