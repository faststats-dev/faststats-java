package dev.faststats.core.flags;

import dev.faststats.core.Token;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * A service for managing feature flags.
 * <p>
 * Use one of the static {@code create} methods to construct a service instance.
 *
 * @since 0.23.0
 */
public sealed interface FeatureFlagService permits SimpleFeatureFlagService {
    /**
     * Creates a feature flag service for the given environment token
     * and a default cache TTL of five minutes.
     *
     * @param token the environment token
     * @return a new feature flag service
     * @see #create(String, Attributes)
     * @since 0.23.0
     */
    @Contract(value = "_ -> new", pure = true)
    static FeatureFlagService create(@Token final String token) {
        return create(token, null);
    }

    /**
     * Creates a feature flag service for the given environment token
     * and global targeting attributes with a default cache TTL of five minutes.
     *
     * @param token      the environment token
     * @param attributes the global targeting attributes
     * @return a new feature flag service
     * @see #create(String, Attributes, Duration)
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static FeatureFlagService create(@Token final String token, @Nullable final Attributes attributes) {
        return create(token, attributes, Duration.ofMinutes(5));
    }

    /**
     * Creates a feature flag service for the given environment token,
     * global targeting attributes, and cache TTL.
     *
     * @param token      the environment token
     * @param attributes the global targeting attributes
     * @param ttl        the cache time-to-live for resolved flag values
     * @return a new feature flag service
     * @throws IllegalArgumentException if the TTL is negative
     * @since 0.23.0
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    static FeatureFlagService create(@Token final String token, @Nullable final Attributes attributes, final Duration ttl) throws IllegalArgumentException {
        return new SimpleFeatureFlagService(token, attributes, ttl);
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
}
