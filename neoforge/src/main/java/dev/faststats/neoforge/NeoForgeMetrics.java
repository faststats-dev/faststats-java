package dev.faststats.neoforge;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

abstract class NeoForgeMetrics extends SimpleMetrics {
    protected final IModInfo mod;

    protected NeoForgeMetrics(final Factory factory, final IModInfo mod) throws IllegalStateException {
        super(factory);
        this.mod = mod;
    }

    protected void appendNeoForgeData(final JsonObject metrics, final String serverType) {
        metrics.addProperty("minecraft_version", modVersion("minecraft"));
        metrics.addProperty("platform_version", modVersion("neoforge"));
        metrics.addProperty("plugin_version", mod.getVersion().toString());
        metrics.addProperty("server_type", serverType);
    }

    private static String modVersion(final String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
