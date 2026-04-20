package dev.faststats.fabric;

import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * Fabric FastStats context.
 *
 * @since 0.23.0
 */
public final class FabricContext extends SimpleContext {
    final ModContainer mod;

    public FabricContext(final String modId, @Token final String token) {
        super(SimpleConfig.read(FabricLoader.getInstance().getConfigDir().resolve("faststats").resolve("config.properties")), token);
        this.mod = FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
    }

    @Override
    public FabricMetrics.Factory metrics() {
        return new FabricMetricsImpl.Factory(this);
    }
}
