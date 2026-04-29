package dev.faststats.fabric;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

final class FabricMetricsClientImpl extends FabricMetricsImpl {
    private @Nullable Minecraft client;

    @Async.Schedule
    @Contract(mutates = "io")
    FabricMetricsClientImpl(final Factory factory, final ModContainer mod) throws IllegalStateException {
        super(factory, mod);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            this.client = client;
            startSubmitting();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        assert client != null : "Client not initialized";
        metrics.addProperty("minecraft_version", SharedConstants.getCurrentVersion().name()); // todo: doublecheck
        metrics.addProperty("online_mode", client.getUser().getXuid().isPresent() && !client.isOfflineDeveloperMode()); // todo: doublecheck
        metrics.addProperty("player_count", getPlayerCount());
        appendFabricData(metrics, "Fabric Client");
    }

    private int getPlayerCount() {
        assert client != null : "Client not initialized";
        final var connection = client.getConnection();
        if (connection != null) return connection.getOnlinePlayers().size();

        final var server = client.getSingleplayerServer();
        if (server != null) return server.getPlayerCount();

        return client.player == null ? 0 : 1;
    }
}
