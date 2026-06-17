package dev.faststats.neoforge;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.Nullable;

final class NeoForgeMetricsServer extends NeoForgeMetrics {
    private @Nullable MinecraftServer server;

    NeoForgeMetricsServer(final Factory factory, final IModInfo mod) throws IllegalStateException {
        super(factory, mod);
        NeoForge.EVENT_BUS.addListener((final ServerStartedEvent event) -> this.server = event.getServer());
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        assert server != null : "Server not initialized";
        metrics.addProperty("online_mode", server.usesAuthentication());
        metrics.addProperty("player_count", server.getPlayerCount());
        appendNeoForgeData(metrics, "NeoForge");
    }
}
