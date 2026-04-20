package dev.faststats.hytale;

import dev.faststats.Metrics;

/**
 * Hytale metrics implementation.
 *
 * @since 0.9.0
 */
public sealed interface HytaleMetrics extends Metrics permits HytaleMetricsImpl {
    interface Factory extends Metrics.Factory<Factory> {
    }
}
