package dev.faststats.minestom;

import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;

import java.nio.file.Path;

/**
 * Minestom FastStats context.
 *
 * @since 0.23.0
 */
public final class MinestomContext extends SimpleContext {
    public MinestomContext(@Token final String token) {
        super(SimpleConfig.read(Path.of("faststats", "config.properties")), token);
    }

    @Override
    public MinestomMetrics.Factory metrics() {
        return new MinestomMetricsImpl.Factory(this);
    }
}
