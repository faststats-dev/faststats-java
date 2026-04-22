package dev.faststats;

import dev.faststats.data.Metric;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

/**
 * Metrics interface.
 *
 * @since 0.23.0
 */
public interface Metrics {
    /**
     * Get the error tracker for this metrics instance.
     *
     * @return the error tracker
     * @since 0.23.0
     */
    @Contract(pure = true)
    Optional<ErrorTracker> getErrorTracker();

    /**
     * Performs additional post-startup tasks.
     * <p>
     * This method may only be called when the application startup is complete.
     * <p>
     * <i>No-op in most implementations.</i>
     *
     * @apiNote Refer to your {@code Metrics} provider's documentation.
     * @since 0.23.0
     */
    default void ready() {
    }

    /**
     * Safely shuts down the metrics submission.
     * <p>
     * This method should be called when the application is shutting down.
     *
     * @since 0.23.0
     */
    @Contract(mutates = "this")
    void shutdown();

    /**
     * A metrics factory.
     *
     * @since 0.23.0
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
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory addMetric(Metric<?> metric) throws IllegalArgumentException;

        /**
         * Sets the flush callback for this metrics instance.
         * <p>
         * This callback will be invoked when the metrics have been submitted to, and accepted by, the metrics server.
         *
         * @param flush the flush callback
         * @return the metrics factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory onFlush(Runnable flush);

        /**
         * Sets the error tracker for this metrics instance.
         * <p>
         * If {@link Config#errorTracking()} is disabled, no errors will be submitted.
         *
         * @param tracker the error tracker
         * @return the metrics factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory errorTracker(ErrorTracker tracker);

        /**
         * Creates a new metrics instance.
         * <p>
         * Metrics submission will start automatically.
         *
         * @return the metrics instance
         * @throws IllegalStateException if the token is not specified
         * @since 0.23.0
         */
        @Async.Schedule
        @Contract(value = " -> new", mutates = "io")
        Metrics create() throws IllegalStateException;
    }

}
