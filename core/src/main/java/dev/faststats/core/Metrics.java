package dev.faststats.core;

import dev.faststats.core.data.Metric;
import dev.faststats.core.flags.FeatureFlagService;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

/**
 * Metrics interface.
 *
 * @since 0.1.0
 */
public interface Metrics {
    /**
     * Get the token used to authenticate with the metrics server and identify the project.
     *
     * @return the metrics token
     * @since 0.1.0
     */
    @Token
    @Contract(pure = true)
    String getToken();

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
         * Sets the token used to authenticate with the metrics server and identify the project.
         * <p>
         * This token can be found in the settings of your project under <b>"Your API Token"</b>.
         *
         * @param token the metrics token
         * @return the metrics factory
         * @throws IllegalArgumentException if the token does not match the {@link Token#PATTERN}
         * @since 0.1.0
         */
        @Contract(mutates = "this")
        F token(@Token String token) throws IllegalArgumentException;

        /**
         * Creates a new metrics instance.
         * <p>
         * Metrics submission will start automatically.
         *
         * @param object a required object as defined by the implementation
         * @return the metrics instance
         * @throws IllegalStateException if the token is not specified
         * @see #token(String)
         * @since 0.1.0
         */
        @Async.Schedule
        @Contract(value = "_ -> new", mutates = "io")
        Metrics create(T object) throws IllegalStateException;
    }

}
