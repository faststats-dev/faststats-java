package dev.faststats.minestom;

import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.data.Metric;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;

/**
 * Minestom metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface MinestomMetrics extends Metrics permits MinestomMetricsImpl {
    /**
     * Registers additional exception handlers.
     *
     * @apiNote This method may only be called after {@link MinecraftServer#init(Auth)}.
     * @since 0.14.0
     */
    @Override
    void ready();

    sealed interface Factory extends Metrics.Factory permits MinestomMetricsImpl.Factory {
        @Override
        Factory addMetric(Metric<?> metric) throws IllegalArgumentException;

        @Override
        Factory onFlush(Runnable flush);

        @Override
        Factory errorTracker(ErrorTracker tracker);

        @Override
        MinestomMetrics create() throws IllegalStateException;
    }
}
