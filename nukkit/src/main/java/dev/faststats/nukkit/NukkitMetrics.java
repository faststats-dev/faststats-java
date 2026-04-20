package dev.faststats.nukkit;

import dev.faststats.Metrics;
import org.jetbrains.annotations.Contract;

/**
 * Nukkit metrics implementation.
 *
 * @since 0.8.0
 */
public sealed interface NukkitMetrics extends Metrics permits NukkitMetricsImpl {
    interface Factory extends Metrics.Factory<Factory> {
    }
}
