package dev.faststats.forge;

import com.google.gson.JsonObject;
import dev.faststats.forge.compat.CompatibilityLayer;

final class ForgeMetricsServer extends ForgeMetrics {
    ForgeMetricsServer(final Factory factory, final String modVersion, final CompatibilityLayer compatibilityLayer) throws IllegalStateException {
        super(factory, modVersion, compatibilityLayer);
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("online_mode", compatibilityLayer.serverOnlineMode());
        metrics.addProperty("player_count", compatibilityLayer.serverPlayerCount());
        appendForgeData(metrics, "Forge");
    }
}
