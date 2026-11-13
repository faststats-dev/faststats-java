package dev.faststats.fabric.compat.v1_18;

import dev.faststats.fabric.compat.CompatibilityLayer;
import dev.faststats.fabric.compat.ServerCompatibilityLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CompatibilityLayerLoader implements CompatibilityLayer.Loader {
    @Override
    public CompatibilityLayer compat() {
        return switch (environment()) {
            case CLIENT -> new ClientCompatibilityLayer();
            case SERVER -> ServerCompatibilityLayer.INSTANCE;
        };
    }

    @Override
    public EnvType environment() {
        return FabricLoader.getInstance().getEnvironmentType();
    }
}
