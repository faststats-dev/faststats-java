package dev.faststats;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shared FastStats context.
 * <p>
 * A context provides a shared configuration, token, metrics, feature flag service,
 * and error tracker service for a project.
 * Platforms create contexts by configuring a {@link SimpleContext.Builder}.
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
     * Performs additional post-startup tasks for configured context services.
     * <p>
     * If a ready hook fails, the failure is always logged, regardless of the debug setting,
     * and the context stays not ready, so this method can be called again later.
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

    /**
     * Builder for {@link FastStatsContext} on platforms that can ask the user for consent,
     * such as game clients.
     * <p>
     * By default, no consent is required.
     *
     * @since 0.33.0
     */
    sealed interface ConsentBuilder extends Builder permits SimpleContext.ConsentBuilder {
        /**
         * Requires the user to explicitly give consent before anything is submitted.
         * <p>
         * When at least one project requires consent, the consent screen is shown
         * until it was shown once, nothing is submitted before that.
         * Platforms without a consent screen, such as dedicated servers, ignore this setting.
         *
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = " -> this", mutates = "this")
        ConsentBuilder requireConsent();

        /**
         * Whether the user has to explicitly give consent before anything is submitted.
         *
         * @return {@code true} if consent is required
         * @since 0.33.0
         */
        @Contract(pure = true)
        boolean isConsentRequired();

        @Override
        ConsentBuilder token(@Token @Nullable String token);

        @Override
        ConsentBuilder onReady(Consumer<FastStatsContext> hook);

        @Override
        ConsentBuilder onShutdown(Consumer<FastStatsContext> hook);

        @Override
        ConsentBuilder errorTrackerService(ErrorTracker errorTracker);

        @Override
        ConsentBuilder metrics(Function<Metrics.Factory, Metrics> metrics);

        @Override
        ConsentBuilder featureFlagService(Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService);
    }

    /**
     * Builder for {@link FastStatsContext}.
     * <p>
     * Platforms configure the platform-specific properties of the builder
     * and delegate the service configuration to their users.
     *
     * @since 0.33.0
     */
    sealed interface Builder permits SimpleContext.Builder, ConsentBuilder {
        /**
         * Sets the FastStats project token.
         * <p>
         * If no token is set, the token injected at build time is read,
         * if a {@linkplain SimpleContext.Builder#tokenResource(ClassLoader, String) token resource} was configured.
         * Without any token, the created context is disabled.
         *
         * @param token the FastStats project token, or {@code null}
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Builder token(@Token final @Nullable String token);

        /**
         * Adds a hook that runs once when the context becomes {@linkplain FastStatsContext#ready() ready}.
         * <p>
         * If a hook fails, the context stays not ready and all hooks run again on the next call,
         * so hooks must be safe to run more than once.
         *
         * @param hook the hook
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Builder onReady(Consumer<FastStatsContext> hook);

        /**
         * Adds a hook that runs when the context is {@linkplain FastStatsContext#shutdown() shut down}.
         *
         * @param hook the hook
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Builder onShutdown(final Consumer<FastStatsContext> hook);

        /**
         * Configures the global/internal error tracker for the context.
         *
         * @param errorTracker the global/internal error tracker
         * @return this builder
         * @since 0.24.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Builder errorTrackerService(final ErrorTracker errorTracker);

        /**
         * Configures and creates the single metrics instance for the context.
         *
         * @param metrics a function that receives a new metrics factory and returns the built metrics instance
         * @return this builder
         * @since 0.24.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Builder metrics(final Function<Metrics.Factory, Metrics> metrics);

        /**
         * Configures and creates the single feature flag service instance for the context.
         *
         * @param featureFlagService a function that receives a new service factory and returns the built service instance
         * @return this builder
         * @since 0.24.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Builder featureFlagService(final Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService);

        /**
         * Builds the configured context.
         *
         * @return the configured context
         * @throws IllegalArgumentException if the token is invalid
         * @throws IllegalStateException    if a required property is missing or no service is configured
         * @throws UncheckedIOException     if an IO error occurs
         * @since 0.33.0
         */
        @Contract(value = " -> new", mutates = "io")
        FastStatsContext build() throws IllegalArgumentException, IllegalStateException, UncheckedIOException;
    }
}
