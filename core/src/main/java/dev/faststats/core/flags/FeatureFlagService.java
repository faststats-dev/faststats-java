package dev.faststats.core.flags;

import dev.faststats.core.Metrics;
import dev.faststats.core.Settings;
import org.jetbrains.annotations.Contract;

import java.time.Duration;

/**
 * A service for managing feature flags.
 * <p>
 * Create an instance using the {@link Factory} and pass it to the metrics factory
 * via {@link Metrics.Factory#featureFlagService(FeatureFlagService)}.
 *
 * @since 0.23.0
 */
public sealed interface FeatureFlagService permits SimpleFeatureFlagService {
    /**
     * Create a new {@link FeatureFlagService} with the given settings and default options.
     *
     * @param settings the SDK-wide settings
     * @return a new feature flag service
     * @since 0.23.0
     */
    @Contract(value = "_ -> new", pure = true)
    static FeatureFlagService create(final Settings settings) {
        return factory().settings(settings).create();
    }

    /**
     * Create a new factory for building a {@link FeatureFlagService}.
     *
     * @return a new factory
     * @since 0.23.0
     */
    @Contract(value = " -> new", pure = true)
    static Factory factory() {
        return new SimpleFeatureFlagService.Factory();
    }

    /**
     * Define a boolean feature flag.
     *
     * @param id           the flag identifier
     * @param defaultValue the default value
     * @return the feature flag
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    FeatureFlag<Boolean> define(String id, boolean defaultValue);

    /**
     * Define a boolean feature flag with per-flag targeting attributes.
     *
     * @param id           the flag identifier
     * @param defaultValue the default value
     * @param attributes   the per-flag targeting attributes, merged with the service attributes
     * @return the feature flag
     * @since 0.23.0
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    FeatureFlag<Boolean> define(String id, boolean defaultValue, Attributes attributes);

    /**
     * Define a string feature flag.
     *
     * @param id           the flag identifier
     * @param defaultValue the default value
     * @return the feature flag
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    FeatureFlag<String> define(String id, String defaultValue);

    /**
     * Define a string feature flag with per-flag targeting attributes.
     *
     * @param id           the flag identifier
     * @param defaultValue the default value
     * @param attributes   the per-flag targeting attributes, merged with the service attributes
     * @return the feature flag
     * @since 0.23.0
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    FeatureFlag<String> define(String id, String defaultValue, Attributes attributes);

    /**
     * Define a number feature flag.
     *
     * @param id           the flag identifier
     * @param defaultValue the default value
     * @return the feature flag
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    FeatureFlag<Number> define(String id, Number defaultValue);

    /**
     * Define a number feature flag with per-flag targeting attributes.
     *
     * @param id           the flag identifier
     * @param defaultValue the default value
     * @param attributes   the per-flag targeting attributes, merged with the service attributes
     * @return the feature flag
     * @since 0.23.0
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    FeatureFlag<Number> define(String id, Number defaultValue, Attributes attributes);

    /**
     * Shuts down the feature flag service.
     *
     * @since 0.23.0
     */
    @Contract(mutates = "this")
    void shutdown();

    /**
     * A factory for creating {@link FeatureFlagService} instances.
     *
     * @since 0.23.0
     */
    interface Factory {
        /**
         * Sets the cache time-to-live for flag values.
         * <p>
         * This TTL determines the staleness window reported by
         * {@link FeatureFlag#getExpiration()}. Expired cached values remain
         * readable until they are explicitly refreshed or invalidated.
         *
         * @param ttl the cache time-to-live
         * @return the factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory ttl(Duration ttl);

        /**
         * Sets the global targeting attributes for all flags created by this service.
         *
         * @param attributes the targeting attributes
         * @return the factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory attributes(Attributes attributes);

        /**
         * Sets the SDK-wide settings for this feature flag service.
         *
         * @param settings the settings
         * @return the factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory settings(Settings settings);

        /**
         * Creates a new {@link FeatureFlagService} instance.
         *
         * @return the feature flag service
         * @throws IllegalStateException if the settings are not specified
         * @see #settings(Settings)
         * @since 0.23.0
         */
        @Contract(value = " -> new", pure = true)
        FeatureFlagService create() throws IllegalStateException;
    }
}
