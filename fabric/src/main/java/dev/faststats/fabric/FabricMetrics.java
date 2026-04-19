package dev.faststats.fabric;

import dev.faststats.core.Metrics;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

/**
 * Fabric metrics implementation.
 *
 * @since 0.12.0
 */
public sealed interface FabricMetrics extends Metrics permits FabricMetricsImpl {
    /**
     * Creates a new metrics factory for Fabric.
     *
     * @return the metrics factory
     * @since 0.12.0
     */
    @Contract(pure = true)
    static Factory factory() {
        return new FabricMetricsImpl.Factory();
    }

    interface Factory extends Metrics.Factory<String, Factory> {
        /**
         * Creates a new metrics instance.
         * <p>
         * Metrics submission will start automatically.
         *
         * @param modId the mod id
         * @return the metrics instance
         * @throws IllegalStateException    if the token is not specified
         * @throws IllegalArgumentException if the mod is not found
         * @see #token(String)
         * @since 0.12.0
         */
        @Override
        @Async.Schedule
        @Contract(value = "_ -> new", mutates = "io")
        Metrics create(String modId) throws IllegalStateException, IllegalArgumentException;
    }
}
