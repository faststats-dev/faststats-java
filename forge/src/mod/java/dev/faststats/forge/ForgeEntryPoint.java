package dev.faststats.forge;

import dev.faststats.forge.compat.CompatibilityLayer;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge requires every mod declared in the {@code mods.toml} to have an entry point annotated with {@link Mod}.
 */
@Mod("faststats")
public final class ForgeEntryPoint {
    public ForgeEntryPoint() {
        if (FastStatsForge.loader().compat() instanceof final CompatibilityLayer.Server server) server.initServer();
    }
}
