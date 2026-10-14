package dev.faststats;

import dev.faststats.data.Metric;
import org.jetbrains.annotations.Contract;

/**
 * Metrics interface.
 *
 * @since 0.24.0
 */
public interface Metrics {
    /**
     * A metrics factory.
     *
     * @since 0.24.0
     */
    interface Factory {
        /**
         * Adds a metric to the metrics submission.
         * <p>
         * If {@link Config#additionalMetrics()} is disabled, the metric will not be submitted.
         *
         * @param metric the metric to add
         * @return the metrics factory
         * @throws IllegalArgumentException if the metric is already added
         * @since 0.24.0
         */
        @Contract(mutates = "this")
        Factory addMetric(Metric<?> metric) throws IllegalArgumentException;

        /**
         * Adds a flush callback to this metrics instance.
         * <p>
         * This callback will be invoked when the metrics have been submitted to, and accepted by, the metrics server.
         *
         * @param flush the flush callback
         * @return the metrics factory
         * @since 0.24.0
         */
        @Contract(mutates = "this")
        Factory onFlush(Runnable flush);

        /**
         * Creates a new metrics instance.
         *
         * @return the metrics instance
         * @throws IllegalStateException if the token is not specified
         * @since 0.24.0
         */
        @Contract(value = " -> new", pure = true)
        Metrics create() throws IllegalStateException;
    }

}
