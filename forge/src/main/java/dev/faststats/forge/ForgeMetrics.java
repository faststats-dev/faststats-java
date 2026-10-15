package dev.faststats.forge;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import dev.faststats.forge.compat.CompatibilityLayer;

abstract class ForgeMetrics extends SimpleMetrics {
    protected final String modVersion;
    protected final CompatibilityLayer compatibilityLayer;

    protected ForgeMetrics(final Factory factory, final String modVersion, final CompatibilityLayer compatibilityLayer) throws IllegalStateException {
        super(factory);
        this.modVersion = modVersion;
        this.compatibilityLayer = compatibilityLayer;
    }

    protected void appendForgeData(final JsonObject metrics, final String serverType) {
        compatibilityLayer.modVersion("minecraft").ifPresent(s -> metrics.addProperty("game_version", s));
        compatibilityLayer.modVersion("forge").ifPresent(s -> metrics.addProperty("platform_version", s));
        metrics.addProperty("plugin_version", modVersion);
        metrics.addProperty("server_type", serverType);
    }
}
