package dev.faststats.neoforge.compat.v26_1;

import dev.faststats.neoforge.compat.CompatibilityLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jspecify.annotations.Nullable;

public final class CompatibilityLayerImpl implements CompatibilityLayer {
    private @Nullable MinecraftServer server;

    @Override
    public void initServer() {
        NeoForge.EVENT_BUS.addListener((final ServerStartedEvent event) -> this.server = event.getServer());
    }

    @Override
    public boolean clientOnlineMode() {
        final var client = Minecraft.getInstance();
        return client.getUser().getXuid().isPresent() && !client.isOfflineDeveloperMode();
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

    @Override
    public Environment getEnvironment() {
        return switch (FMLEnvironment.getDist()) {
            case CLIENT -> Environment.CLIENT;
            case DEDICATED_SERVER -> Environment.SERVER;
        };
    }
}
