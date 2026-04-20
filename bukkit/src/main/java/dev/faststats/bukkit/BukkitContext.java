package dev.faststats.bukkit;

import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;

/**
 * Bukkit FastStats context.
 *
 * @since 0.23.0
 */
public final class BukkitContext extends SimpleContext {
    final Plugin plugin;
    
    public BukkitContext(final Plugin plugin, @Token final String token) {
        super(SimpleConfig.read(getConfigPath(plugin)), token);
        this.plugin = plugin;
    }

    @Override
    public BukkitMetrics.Factory metrics() {
        return new BukkitMetricsImpl.Factory(this);
    }

    private static Path getConfigPath(final Plugin plugin) {
        return getPluginsFolder(plugin).resolve("faststats").resolve("config.properties");
    }

    private static Path getPluginsFolder(final Plugin plugin) {
        try {
            return plugin.getServer().getPluginsFolder().toPath();
        } catch (final NoSuchMethodError e) {
            return plugin.getDataFolder().getParentFile().toPath();
        }
    }
}
