package dev.faststats.forge;

import com.google.gson.JsonObject;
import dev.faststats.forge.compat.CompatibilityLayer;

final class ForgeMetricsClient extends ForgeMetrics {
    ForgeMetricsClient(final Factory factory, final String modVersion, final CompatibilityLayer compatibilityLayer) throws IllegalStateException {
        super(factory, modVersion, compatibilityLayer);
    }

    @Override
    public boolean isClientApplication() {
        return true;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", compatibilityLayer.clientOnlineMode());
        metrics.addProperty("player_count", compatibilityLayer.clientPlayerCount());
        appendForgeData(metrics, "Forge Client");
    }
}
