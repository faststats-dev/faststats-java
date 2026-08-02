package dev.faststats.neoforge;

import com.google.gson.JsonObject;
import dev.faststats.neoforge.compat.CompatibilityLayer;
import net.neoforged.neoforgespi.language.IModInfo;

final class NeoForgeMetricsClient extends NeoForgeMetrics {
    private final CompatibilityLayer compatibilityLayer;

    NeoForgeMetricsClient(final Factory factory, final IModInfo mod, final CompatibilityLayer compatibilityLayer) throws IllegalStateException {
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
        appendNeoForgeData(metrics, "NeoForge Client");
    }
}
