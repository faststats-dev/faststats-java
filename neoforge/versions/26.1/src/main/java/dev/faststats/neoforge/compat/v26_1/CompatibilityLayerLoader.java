package dev.faststats.neoforge.compat.v26_1;

import dev.faststats.neoforge.compat.CompatibilityLayer;
import dev.faststats.neoforge.compat.ServerCompatibilityLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CompatibilityLayerLoader implements CompatibilityLayer.Loader {
    @Override
    public CompatibilityLayer compat() {
        return switch (environment()) {
            case CLIENT -> new ClientCompatibilityLayer();
            case DEDICATED_SERVER -> ServerCompatibilityLayer.INSTANCE;
        };
    }

    @Override
    public Dist environment() {
        return FMLEnvironment.getDist();
    }
}
