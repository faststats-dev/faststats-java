package dev.faststats.forge.compat.v26_1;

import dev.faststats.forge.compat.CompatibilityLayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.Optional;

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
    public CompatibilityLayer.Environment environment() {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> CompatibilityLayer.Environment.CLIENT;
            case DEDICATED_SERVER -> CompatibilityLayer.Environment.SERVER;
        };
    }

    @Override
    public Optional<String> modVersion(final String modId) {
        return ModList.getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
