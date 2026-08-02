package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.fabric.compat.CompatibilityLayer;
import net.fabricmc.loader.api.ModContainer;

final class FabricMetricsClient extends FabricMetrics {
    private final CompatibilityLayer compatibilityLayer;

    FabricMetricsClient(final Factory factory, final ModContainer mod, final CompatibilityLayer compatibilityLayer) throws IllegalStateException {
        super(factory, mod);
        this.compatibilityLayer = compatibilityLayer;
    }

    @Override
    public boolean isClientApplication() {
        return true;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", compatibilityLayer.clientOnlineMode());
        metrics.addProperty("player_count", compatibilityLayer.clientPlayerCount());
        appendFabricData(metrics, "Fabric Client");
    }
}
