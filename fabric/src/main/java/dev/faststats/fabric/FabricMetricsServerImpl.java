package dev.faststats.fabric;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

final class FabricMetricsServerImpl extends FabricMetricsImpl {
    private @Nullable MinecraftServer server;

    @Async.Schedule
    @Contract(mutates = "io")
    FabricMetricsServerImpl(final Factory factory, final ModContainer mod) throws IllegalStateException {
        super(factory, mod);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            startSubmitting();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutdown());
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        assert server != null : "Server not initialized";
        metrics.addProperty("minecraft_version", server.getServerVersion());
        metrics.addProperty("online_mode", server.usesAuthentication());
        metrics.addProperty("player_count", server.getPlayerCount());
        appendFabricData(metrics, "Fabric");
    }
}
