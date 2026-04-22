package dev.faststats;

import org.jetbrains.annotations.Contract;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A feature flag.
 * <p>
 * Feature flags are defined via {@link FeatureFlagService#define} and are bound to
 * the service's cache and lifecycle.
 *
 * @param <T> the flag value type
 * @since 0.23.0
 */
public sealed interface FeatureFlag<T> permits SimpleFeatureFlag {
    /**
     * Get the flag identifier.
     *
     * @return the flag id
     * @since 0.23.0
     */
    @Contract(pure = true)
    String getId();

    /**
     * Returns the type representing the value type of this flag.
     *
     * @return the value type class
     * @since 0.23.0
     */
    @Contract(pure = true)
    Type getType();

    /**
     * Returns the class representing the value type of this flag.
     * <p>
     * This always returns exactly one of {@link String}.class,
     * {@link Number}.class, or {@link Boolean}.class, matching {@link #getType()}.
     *
     * @return the value type class
     * @since 0.23.0
     */
    @Contract(pure = true)
    Class<T> getTypeClass();

    /**
     * Get the current cached flag value.
     * <p>
     * This method is non-blocking and never performs a network request. It
     * returns {@link Optional#empty()} until a value has been fetched and
     * stored locally.
     *
     * @return the cached value, if present
     * @since 0.23.0
     */
    @Contract(pure = true)
    Optional<T> getCached();

    /**
     * Get the expiration time for the current cached value.
     * <p>
     * If no value has been cached yet, this returns {@link Optional#empty()}.
     * The returned timestamp indicates when the cached value should be treated
     * as stale according to the configured TTL.
     *
     * @return the expiration time of the cached value, if present
     * @see #isValid()
     * @since 0.23.0
     */
    @Contract(pure = true)
    Optional<Instant> getExpiration();

    /**
     * Returns whether the current cached value is expired.
     * <p>
     * A value is expired when no fetch has completed yet or when the
     * configured TTL has elapsed since the last fetch.
     *
     * @return {@code true} if the cached value is absent or stale
     * @see #getExpiration()
     * @see #isValid()
     * @since 0.23.0
     */
    @Contract(pure = true)
    boolean isExpired();

    /**
     * Returns whether the current cached value is still valid.
     * <p>
     * A value is valid when it is cached and its configured TTL has not yet
     * expired. This method is non-blocking and never performs a network
     * request.
     *
     * @return {@code true} if a non-expired cached value is available
     * @see #getExpiration()
     * @since 0.23.0
     */
    @Contract(pure = true)
    boolean isValid();

    /**
     * Return a future that completes with the flag value once it is ready.
     * <p>
     * If the value is valid according to {@link #isValid()},
     * the returned future completes immediately. Otherwise, a new fetch is
     * performed and the future completes when the response arrives.
     *
     * @return a future completing with the flag value
     * @see #fetch()
     * @since 0.23.0
     */
    CompletableFuture<T> whenReady();

    /**
     * Force a fresh fetch of the flag value from the server.
     * <p>
     * Unlike {@link #whenReady()}, this always performs a server request.
     * <p>
     * The returned future may complete exceptionally if the request fails, the
     * server returns a non-successful response, or the response body cannot be
     * parsed as the requested flag type. The most common non-successful response
     * is an unknown flag identifier.
     * <p>
     * Failed fetches do not update the cached value. Failure details are only
     * logged when debug logging is enabled.
     *
     * @return a future completing with the latest server value
     * @see #whenReady()
     * @since 0.23.0
     */
    @Contract(mutates = "this")
    CompletableFuture<T> fetch();

    /**
     * Request that the server opt in to this flag, then invalidate the local
     * value and fetch the current server value again.
     * <p>
     * This sends a {@code POST /v1/flag/opt-in} request. The server determines
     * the resulting flag value based on its own conditions.
     * <p>
     * The returned future completes with the updated value after the local
     * cache has been reset and the follow-up fetch finishes.
     *
     * @return a future completing with the updated flag value
     * @see #fetch()
     * @since 0.23.0
     */
    @Contract(mutates = "this")
    CompletableFuture<T> optIn();

    /**
     * Request that the server opt out of this flag, then invalidate the local
     * value and fetch the current server value again.
     * <p>
     * This sends a {@code POST /v1/flag/opt-out} request.
     * <p>
     * The returned future completes with the updated value after the local
     * cache has been reset and the follow-up fetch finishes.
     *
     * @return a future completing with the updated flag value
     * @see #fetch()
     * @since 0.23.0
     */
    @Contract(mutates = "this")
    CompletableFuture<T> optOut();

    /**
     * Get the default value for this flag.
     *
     * @return the default value
     * @since 0.23.0
     */
    @Contract(pure = true)
    T getDefaultValue();

    /**
     * Supported value types for feature flags.
     *
     * @since 0.23.0
     */
    enum Type {
        /**
         * A string-valued flag.
         *
         * @since 0.23.0
         */
        STRING,

        /**
         * A boolean-valued flag.
         *
         * @since 0.23.0
         */
        BOOLEAN,

        /**
         * A numeric flag.
         *
         * @since 0.23.0
         */
        NUMBER
    }
}
