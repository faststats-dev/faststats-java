package dev.faststats.bungee;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Contract;

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
    @Contract(value = " -> new", pure = true)
    public Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new BungeeMetricsImpl(this, plugin);
            }
        };
    }
}
