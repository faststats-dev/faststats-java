package dev.faststats.fabric.compat.v1_16_1;

import dev.faststats.fabric.compat.CompatibilityLayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

public final class CompatibilityLayerImpl implements CompatibilityLayer {
    private @Nullable MinecraftServer server;

    @Override
    public void initServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);
    }

    @Override
    public boolean clientOnlineMode() {
        final var client = Minecraft.getInstance();
        return client.getUser().getGameProfile().getId().version() == 4; // todo: improve this check
    }

    @Override
    public int clientPlayerCount() {
        final var client = Minecraft.getInstance();
        final var connection = client.getConnection();
        if (connection != null) return connection.getOnlinePlayers().size();

        final var server = client.getSingleplayerServer();
        if (server != null) return server.getPlayerCount();

        return client.player == null ? 0 : 1;
    }

    @Override
    public boolean serverOnlineMode() {
        assert server != null : "Server not initialized";
        return server.usesAuthentication();
    }

    @Override
    public int serverPlayerCount() {
        assert server != null : "Server not initialized";
        return server.getPlayerCount();
    }
}
