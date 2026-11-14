package dev.faststats.neoforge;

import dev.faststats.neoforge.compat.CompatibilityLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Mod(value = "faststats", dist = Dist.DEDICATED_SERVER)
public final class FastStatsServerEntrypoint {
    public FastStatsServerEntrypoint() {
        if (FastStatsNeoForge.loader().compat() instanceof final CompatibilityLayer.Server server) server.initServer();
    }
}
