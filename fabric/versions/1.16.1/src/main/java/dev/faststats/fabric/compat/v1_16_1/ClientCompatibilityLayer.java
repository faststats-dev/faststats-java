package dev.faststats.fabric.compat.v1_16_1;

import dev.faststats.fabric.compat.CompatibilityLayer;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class ClientCompatibilityLayer implements CompatibilityLayer.Client {
    @Override
    public boolean onlineMode() {
        final var client = Minecraft.getInstance();
        return client.getUser().getGameProfile().getId().version() == 4; // todo: improve this check
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
