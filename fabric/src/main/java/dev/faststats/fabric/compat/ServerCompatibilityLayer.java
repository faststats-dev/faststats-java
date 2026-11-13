package dev.faststats.fabric.compat;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

public final class ServerCompatibilityLayer extends CompatibilityLayer.Server {
    public static final ServerCompatibilityLayer INSTANCE = new ServerCompatibilityLayer();

    private volatile @Nullable MinecraftServer server;

    private ServerCompatibilityLayer() {
    }

    @Override
    public void initServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            onStarted();
        });
    }

    @Override
    public boolean onlineMode() {
        final var server = this.server;
        assert server != null : "Server not initialized";
        return server.usesAuthentication();
    }

    @Override
    public int playerCount() {
        final var server = this.server;
        assert server != null : "Server not initialized";
        return server.getPlayerCount();
    }
}
