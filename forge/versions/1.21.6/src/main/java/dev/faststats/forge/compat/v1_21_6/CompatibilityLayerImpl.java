package dev.faststats.forge.compat.v1_21_6;

import dev.faststats.forge.compat.CompatibilityLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

public final class CompatibilityLayerImpl implements CompatibilityLayer {
    private @Nullable MinecraftServer server;

    @Override
    public void onServerLifecycle(final Runnable started, final Runnable stopping) {
        ServerStartedEvent.BUS.addListener(event -> {
            this.server = event.getServer();
            started.run();
        });
        ServerStoppingEvent.BUS.addListener(event -> stopping.run());
    }

    @Override
    public boolean clientOnlineMode() {
        final var client = Minecraft.getInstance();
        return client.getUser().getXuid().isPresent(); // todo: offline dev mode?
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
    public Optional<String> modVersion(final String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Environment getEnvironment() {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> Environment.CLIENT;
            case DEDICATED_SERVER -> Environment.SERVER;
        };
    }
}
