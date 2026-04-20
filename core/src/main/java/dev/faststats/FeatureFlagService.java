package dev.faststats;

import org.jetbrains.annotations.Contract;

import java.time.Duration;
import java.util.Optional;

/**
 * A service for managing feature flags.
 * <p>
 * Use one of the static {@code create} methods to construct a service instance.
 *
 * @since 0.23.0
 */
public sealed interface FeatureFlagService permits SimpleFeatureFlagService {
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
     * Returns the global targeting attributes configured for this service.
     * <p>
     * These attributes apply to every flag defined by the service and are
     * merged with any per-flag attributes supplied during definition.
     *
     * @return the global targeting attributes, if configured
     * @since 0.23.0
     */
    @Contract(pure = true)
    Optional<Attributes> getAttributes();

    /**
     * Returns the cache time-to-live used for resolved flag values.
     *
     * @return the configured cache time-to-live
     * @since 0.23.0
     */
    Duration getTTL();

    /**
     * Shuts down the feature flag service.
     *
     * @since 0.23.0
     */
    @Contract(mutates = "this")
    void shutdown();
}
