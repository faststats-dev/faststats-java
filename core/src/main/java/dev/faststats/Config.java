package dev.faststats;

import org.jetbrains.annotations.Contract;

import java.util.UUID;

/**
 * A representation of the metrics configuration.
 *
 * @since 0.23.0
 */
public interface Config {
    /**
     * The server id.
     *
     * @return the server id
     * @since 0.23.0
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
     * @since 0.23.0
     */
    @Contract(pure = true)
    boolean enabled();

    /**
     * Whether error tracking is enabled across all metrics instances.
     *
     * @return {@code true} if error tracking is enabled, {@code false} otherwise
     * @since 0.23.0
     */
    @Contract(pure = true)
    boolean errorTracking();

    /**
     * Whether additional metrics are enabled across all metrics instances.
     *
     * @return {@code true} if additional metrics are enabled, {@code false} otherwise
     * @since 0.23.0
     */
    @Contract(pure = true)
    boolean additionalMetrics();

    /**
     * Whether debug logging is enabled across all metrics instances.
     *
     * @return {@code true} if debug logging is enabled, {@code false} otherwise
     * @since 0.23.0
     */
    @Contract(pure = true)
    boolean debug();
}
