package dev.faststats.core;

import dev.faststats.core.data.Metric;
import dev.faststats.core.flags.FeatureFlagService;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.UUID;

/**
 * Metrics interface.
 *
 * @since 0.1.0
 */
public interface Metrics {
    /**
     * Get the SDK-wide settings for this metrics instance.
     *
     * @return the settings
     * @since 0.23.0
     */
    @Contract(pure = true)
    Settings getSettings();

    /**
     * Get the error tracker for this metrics instance.
     *
     * @return the error tracker
     * @since 0.10.0
     */
    @Contract(pure = true)
    Optional<ErrorTracker> getErrorTracker();

    /**
     * Get the feature flag service for this metrics instance.
     *
     * @return the feature flag service
     * @since 0.23.0
     */
    @Contract(pure = true)
    Optional<FeatureFlagService> getFeatureFlagService();

    /**
     * Get the metrics configuration.
     *
     * @return the metrics configuration
     * @since 0.1.0
     */
    @Contract(pure = true)
    Config getConfig();

    /**
     * Performs additional post-startup tasks.
     * <p>
     * This method may only be called when the application startup is complete.
     * <p>
     * <i>No-op in most implementations.</i>
     *
     * @apiNote Refer to your {@code Metrics} provider's documentation.
     * @since 0.14.0
     */
    default void ready() {
    }

    /**
     * Safely shuts down the metrics submission.
     * <p>
     * This method should be called when the application is shutting down.
     *
     * @since 0.1.0
     */
    @Contract(mutates = "this")
    void shutdown();

    /**
     * A metrics factory.
     *
     * @since 0.1.0
     */
    interface Factory<T, F extends Factory<T, F>> {
        /**
         * Adds a metric to the metrics submission.
         * <p>
         * If {@link Config#additionalMetrics()} is disabled, the metric will not be submitted.
         *
         * @param metric the metric to add
         * @return the metrics factory
         * @throws IllegalArgumentException if the metric is already added
         * @since 0.16.0
         */
        @Contract(mutates = "this")
        F addMetric(Metric<?> metric) throws IllegalArgumentException;

        /**
         * Sets the flush callback for this metrics instance.
         * <p>
         * This callback will be invoked when the metrics have been submitted to, and accepted by, the metrics server.
         *
         * @param flush the flush callback
         * @return the metrics factory
         * @since 0.15.0
         */
        @Contract(mutates = "this")
        F onFlush(Runnable flush);

        /**
         * Sets the error tracker for this metrics instance.
         * <p>
         * If {@link Config#errorTracking()} is disabled, no errors will be submitted.
         *
         * @param tracker the error tracker
         * @return the metrics factory
         * @since 0.10.0
         */
        @Contract(mutates = "this")
        F errorTracker(ErrorTracker tracker);

        /**
         * Sets the feature flag service for this metrics instance.
         *
         * @param service the feature flag service
         * @return the metrics factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        F featureFlagService(FeatureFlagService service);

        /**
         * Sets the SDK-wide settings for this metrics instance.
         *
         * @param settings the settings
         * @return the metrics factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        F settings(Settings settings);

        /**
         * Creates a new metrics instance.
         * <p>
         * Metrics submission will start automatically.
         *
         * @param object a required object as defined by the implementation
         * @return the metrics instance
         * @throws IllegalStateException if the settings are not specified
         * @see #settings(Settings)
         * @since 0.1.0
         */
        @Async.Schedule
        @Contract(value = "_ -> new", mutates = "io")
        Metrics create(T object) throws IllegalStateException;
    }

    /**
     * A representation of the metrics configuration.
     *
     * @since 0.1.0
     */
    sealed interface Config permits SimpleMetrics.Config {
        /**
         * The server id.
         *
         * @return the server id
         * @since 0.1.0
         */
        @Contract(pure = true)
        UUID serverId();

        /**
         * Whether metrics submission is enabled.
         * <p>
         * <b>Bypassing this setting may get your project banned from FastStats.</b><br>
         * <b>Users have to be able to opt out from metrics submission.</b>
         *
         * @return {@code true} if metrics submission is enabled, {@code false} otherwise
         * @since 0.1.0
         */
        @Contract(pure = true)
        boolean enabled();

        /**
         * Whether error tracking is enabled across all metrics instances.
         *
         * @return {@code true} if error tracking is enabled, {@code false} otherwise
         * @since 0.11.0
         */
        @Contract(pure = true)
        boolean errorTracking();

        /**
         * Whether additional metrics are enabled across all metrics instances.
         *
         * @return {@code true} if additional metrics are enabled, {@code false} otherwise
         * @since 0.11.0
         */
        @Contract(pure = true)
        boolean additionalMetrics();

        /**
         * Whether debug logging is enabled across all metrics instances.
         *
         * @return {@code true} if debug logging is enabled, {@code false} otherwise
         * @since 0.1.0
         */
        @Contract(pure = true)
        boolean debug();
    }
}
