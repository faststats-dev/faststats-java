package dev.faststats.bungee;

import dev.faststats.Metrics;
import org.jetbrains.annotations.Contract;

/**
 * BungeeCord metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface BungeeMetrics extends Metrics permits BungeeMetricsImpl {
    interface Factory extends Metrics.Factory<Factory> {
    }
}
