package dev.faststats.fabric;

import dev.faststats.fabric.compat.CompatibilityLayer;
import net.fabricmc.api.DedicatedServerModInitializer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class FastStatsServerEntrypoint implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        if (FastStatsFabric.loader().compat() instanceof final CompatibilityLayer.Server server) server.initServer();
    }
}
