package dev.faststats.sponge;

import dev.faststats.FastStatsContext;
import dev.faststats.Metrics;

/**
 * Sponge metrics implementation.
 *
 * @since 0.12.0
 */
public sealed interface SpongeMetrics extends Metrics permits SpongeMetricsImpl {
    final class Factory extends SpongeMetricsImpl.Factory {
        public Factory(final FastStatsContext context) {
            super(context);
        }
    }
}
