package dev.faststats.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;

/**
 * Hytale FastStats context.
 *
 * @since 0.23.0
 */
public final class HytaleContext extends SimpleContext {
    public HytaleContext(final JavaPlugin plugin, @Token final String token) {
        super(SimpleConfig.read(plugin.getDataDirectory().toAbsolutePath().getParent().resolve("faststats").resolve("config.properties")), token);
    }

    @Override
    public Metrics.Factory metrics() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                // todo: add client support?
                return new HytaleMetricsImpl(this);
            }
        };
    }
}
