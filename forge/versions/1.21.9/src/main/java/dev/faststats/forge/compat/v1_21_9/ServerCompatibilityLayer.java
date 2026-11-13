package dev.faststats.forge.compat.v1_21_9;

import dev.faststats.forge.compat.CompatibilityLayer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class ServerCompatibilityLayer extends CompatibilityLayer.Server {
    static final ServerCompatibilityLayer INSTANCE = new ServerCompatibilityLayer();

    private ServerCompatibilityLayer() {
    }

    @Override
    public void initServer() {
        ServerStartedEvent.BUS.addListener(event -> onStarted());
    }

    @Override
    public void whenStopping(final Runnable task) {
        ServerStoppingEvent.BUS.addListener(event -> task.run());
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
