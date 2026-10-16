package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.fabric.compat.CompatibilityLayer;
import net.fabricmc.loader.api.ModContainer;

final class FabricMetricsServer extends FabricMetrics {
    private final CompatibilityLayer compatibilityLayer;

    FabricMetricsServer(final Factory factory, final ModContainer mod, final CompatibilityLayer compatibilityLayer) throws IllegalStateException {
        super(factory, mod);
        this.compatibilityLayer = compatibilityLayer;
        compatibilityLayer.initServer();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", compatibilityLayer.serverOnlineMode());
        metrics.addProperty("player_count", compatibilityLayer.serverPlayerCount());
        appendFabricData(metrics, false);
    }
}
