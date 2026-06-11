package dev.faststats;

import org.jetbrains.annotations.Contract;

import java.util.Optional;

/**
 * Shared FastStats context.
 * <p>
 * Platform-specific contexts should extend this class to provide a shared
 * configuration, token, metrics, feature flag service, and error tracker service
 * for their environment.
 *
 * @since 0.24.0
 */
public sealed interface FastStatsContext permits SimpleContext {
    /**
     * Get the metrics configuration shared by services created from this context.
     *
     * @return the shared configuration
     * @since 0.24.0
     */
    @Contract(pure = true)
    Config getConfig();

    /**
     * Get the token shared by services created from this context.
     *
     * @return the shared token
     * @since 0.24.0
     */
    @Token
    @Contract(pure = true)
    String getToken();

    /**
     * Gets the metrics instance bound to this context.
     *
     * @return the context metrics instance, if one was configured
     * @since 0.24.0
     */
    @Contract(pure = true)
    Optional<Metrics> metrics();

    /**
     * Gets the feature flag service bound to this context.
     *
     * @return the context feature flag service, if one was configured
     * @since 0.24.0
     */
    @Contract(pure = true)
    Optional<FeatureFlagService> featureFlagService();

    /**
     * Gets the error tracker service bound to this context.
     *
     * @return the context error tracker service, if one was configured
     * @since 0.24.0
     */
    @Contract(pure = true)
    Optional<ErrorTrackerService> errorTrackerService();

    /**
     * Gets the performance analyzer bound to this context.
     *
     * @return the context performance analyzer, if one was configured
     * @since 0.27.0
     */
    @Contract(pure = true)
    Optional<PerformanceAnalyzer> performanceAnalyzer();

    /**
     * Performs additional post-startup tasks for configured context services.
     *
     * @since 0.24.0
     */
    void ready();

    /**
     * Safely shuts down configured context services.
     *
     * @since 0.24.0
     */
    @Contract(mutates = "this")
    void shutdown();

    /**
     * Get the SDK information shared by services created from this context.
     *
     * @return the shared SDK information
     * @since 0.24.0
     */
    @Contract(pure = true)
    SdkInfo getSdkInfo();
}
