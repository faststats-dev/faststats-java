package dev.faststats.nukkit;

import cn.nukkit.plugin.PluginBase;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;

import java.nio.file.Path;

/**
 * Nukkit FastStats context.
 *
 * @since 0.23.0
 */
public final class NukkitContext extends SimpleContext {
    final PluginBase plugin;

    public NukkitContext(final PluginBase plugin, @Token final String token) {
        super(SimpleConfig.read(Path.of(plugin.getServer().getPluginPath(), "faststats", "config.properties")), token);
        this.plugin = plugin;
    }

    @Override
    public Metrics.Factory metrics() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new NukkitMetricsImpl(this, ((NukkitContext) context).plugin);
            }
        };
    }
}
