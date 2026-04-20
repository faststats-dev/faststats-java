package dev.faststats.fabric;

import dev.faststats.Metrics;

/**
 * Fabric metrics implementation.
 *
 * @since 0.12.0
 */
public sealed interface FabricMetrics extends Metrics permits FabricMetricsImpl {
    interface Factory extends Metrics.Factory<Factory> {
    }
}
