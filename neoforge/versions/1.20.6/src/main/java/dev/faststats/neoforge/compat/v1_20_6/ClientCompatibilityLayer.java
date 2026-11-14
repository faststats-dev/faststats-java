package dev.faststats.neoforge.compat.v1_20_6;

import dev.faststats.neoforge.compat.CompatibilityLayer;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class ClientCompatibilityLayer implements CompatibilityLayer.Client {
    @Override
    public boolean onlineMode() {
        final var client = Minecraft.getInstance();
        return client.getUser().getXuid().isPresent(); // todo: offline dev mode?
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
