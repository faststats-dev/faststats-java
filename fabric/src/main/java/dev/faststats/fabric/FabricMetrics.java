package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Optional;

abstract class FabricMetrics extends SimpleMetrics {
    protected final ModContainer mod;

    protected FabricMetrics(final Factory factory, final ModContainer mod) throws IllegalStateException {
        super(factory);
        this.mod = mod;
    }

    protected void appendFabricData(final JsonObject metrics, final boolean client) {
        final var loader = isQuilt() ? "Quilt" : "Fabric";
        version("minecraft").ifPresent(s -> metrics.addProperty("game_version", s));
        platformVersion().ifPresent(s -> metrics.addProperty("platform_version", s));
        metrics.addProperty("plugin_version", mod.getMetadata().getVersion().getFriendlyString());
        metrics.addProperty("server_type", client ? loader + " Client" : loader);
    }

    protected static Optional<String> platformVersion() {
        return version(isQuilt() ? "quilt_loader" : "fabricloader");
    }

    private static boolean isQuilt() {
        return FabricLoader.getInstance().isModLoaded("quilt_loader");
    }

    private static Optional<String> version(final String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }
}
