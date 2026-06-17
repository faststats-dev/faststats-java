package dev.faststats.neoforge;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.Nullable;

final class NeoForgeMetricsServer extends NeoForgeMetrics {
    NeoForgeMetricsServer(final Factory factory, final IModInfo mod) throws IllegalStateException {
        super(factory, mod);
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        final @Nullable MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        assert server != null : "Server not initialized";
        metrics.addProperty("online_mode", server.usesAuthentication());
        metrics.addProperty("player_count", server.getPlayerCount());
        appendNeoForgeData(metrics, "NeoForge");
    }
}
