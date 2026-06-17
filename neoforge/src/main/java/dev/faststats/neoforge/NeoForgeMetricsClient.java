package dev.faststats.neoforge;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforgespi.language.IModInfo;

final class NeoForgeMetricsClient extends NeoForgeMetrics {
    NeoForgeMetricsClient(final Factory factory, final IModInfo mod) throws IllegalStateException {
        super(factory, mod);
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        final var client = Minecraft.getInstance();
        metrics.addProperty("online_mode", client.getUser().getXuid().isPresent() && !client.isOfflineDeveloperMode());
        metrics.addProperty("player_count", getPlayerCount(client));
        appendNeoForgeData(metrics, "NeoForge Client");
    }

    private static int getPlayerCount(final Minecraft client) {
        final var connection = client.getConnection();
        if (connection != null) return connection.getOnlinePlayers().size();

        final var server = client.getSingleplayerServer();
        if (server != null) return server.getPlayerCount();

        return client.player == null ? 0 : 1;
    }
}
