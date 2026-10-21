package dev.faststats.neoforge;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.Optional;

abstract class NeoForgeMetrics extends SimpleMetrics {
    protected final IModInfo mod;

    protected NeoForgeMetrics(final Factory factory, final IModInfo mod) throws IllegalStateException {
        super(factory);
        this.mod = mod;
    }

    protected void appendNeoForgeData(final JsonObject metrics, final String serverType) {
        modVersion("minecraft").ifPresent(s -> metrics.addProperty("game_version", s));
        modVersion("neoforge").ifPresent(s -> metrics.addProperty("platform_version", s));
        metrics.addProperty("plugin_version", mod.getVersion().toString());
        metrics.addProperty("server_type", serverType);
    }

    private static Optional<String> modVersion(final String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }
}
