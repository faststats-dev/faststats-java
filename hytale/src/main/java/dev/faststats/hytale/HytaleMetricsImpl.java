package dev.faststats.hytale;

import com.google.gson.JsonObject;
import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.faststats.SimpleMetrics;

final class HytaleMetricsImpl extends SimpleMetrics {
    private final String pluginVersion;
    
    HytaleMetricsImpl(final Factory factory, final JavaPlugin plugin) throws IllegalStateException {
        super(factory);
        this.pluginVersion = plugin.getManifest().getVersion().toString();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("game_version", ManifestUtil.getImplementationVersion());
        metrics.addProperty("online_mode", ServerAuthManager.getInstance().getAuthMode() != ServerAuthManager.AuthMode.NONE);
        metrics.addProperty("platform_version", ManifestUtil.getVersion());
        metrics.addProperty("player_count", Universe.get().getPlayerCount());
        metrics.addProperty("server_type", "Hytale Server");
        metrics.addProperty("plugin_version", pluginVersion);
    }
}
