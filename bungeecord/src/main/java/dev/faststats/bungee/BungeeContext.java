package dev.faststats.bungee;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord FastStats context.
 *
 * @since 0.23.0
 */
public final class BungeeContext extends SimpleContext {
    final Plugin plugin;

    public BungeeContext(final Plugin plugin, @Token final String token) {
        super(SimpleConfig.read(plugin.getProxy().getPluginsFolder().toPath().resolve("faststats").resolve("config.properties")), token);
        this.plugin = plugin;
    }

    @Override
    public Metrics.Factory metrics() {
        return new BungeeMetricsImpl.Factory(this);
    }
}
