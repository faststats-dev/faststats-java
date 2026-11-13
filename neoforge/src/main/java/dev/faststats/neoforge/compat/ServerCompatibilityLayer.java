package dev.faststats.neoforge.compat;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ServerCompatibilityLayer extends CompatibilityLayer.Server {
    public static final ServerCompatibilityLayer INSTANCE = new ServerCompatibilityLayer();

    private ServerCompatibilityLayer() {
    }

    @Override
    public void initServer() {
        NeoForge.EVENT_BUS.addListener((final ServerStartedEvent event) -> onStarted());
    }

    @Override
    public boolean onlineMode() {
        final var server = ServerLifecycleHooks.getCurrentServer();
        assert server != null : "Server not initialized";
        return server.usesAuthentication();
    }

    @Override
    public int playerCount() {
        final var server = ServerLifecycleHooks.getCurrentServer();
        assert server != null : "Server not initialized";
        return server.getPlayerCount();
    }
}
