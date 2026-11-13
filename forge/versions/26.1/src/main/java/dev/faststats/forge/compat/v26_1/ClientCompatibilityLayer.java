package dev.faststats.forge.compat.v26_1;

import dev.faststats.forge.compat.CompatibilityLayer;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class ClientCompatibilityLayer implements CompatibilityLayer.Client {
    @Override
    public boolean onlineMode() {
        final var client = Minecraft.getInstance();
        return client.getUser().getXuid().isPresent() && !client.isOfflineDeveloperMode();
    }

    @Override
    public int playerCount() {
        final var client = Minecraft.getInstance();
        final var connection = client.getConnection();
        if (connection != null) return connection.getOnlinePlayers().size();

        final var server = client.getSingleplayerServer();
        if (server != null) return server.getPlayerCount();

        return client.player == null ? 0 : 1;
    }
}
