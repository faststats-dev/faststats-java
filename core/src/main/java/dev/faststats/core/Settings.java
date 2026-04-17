package dev.faststats.core;

import org.jetbrains.annotations.Contract;

import java.net.URI;

/**
 * SDK-wide settings shared across all FastStats services.
 *
 * @since 0.23.0
 */
public sealed interface Settings permits SimpleSettings {
    /**
     * Creates a new {@link Settings} instance with the given token.
     * <p>
     * This token can be found in the settings of your project under <b>"Your API Token"</b>.
     * It is used to authenticate with the server and identify the project.
     *
     * @param token the token
     * @return the new settings
     * @since 0.23.0
     */
    @Contract(value = "_ -> new", pure = true)
    static Settings withToken(@Token final String token) {
        return factory().token(token).create();
    }

    /**
     * Create a new factory for building {@link Settings}.
     *
     * @return a new factory
     * @since 0.23.0
     */
    @Contract(value = " -> new", pure = true)
    static Factory factory() {
        return new SimpleSettings.Factory();
    }

    /**
     * The token used to authenticate with the server and identify the project.
     *
     * @return the token
     * @since 0.23.0
     */
    @Token
    @Contract(pure = true)
    String token();

    /**
     * The server URL.
     *
     * @return the server URL
     * @since 0.23.0
     */
    @Contract(pure = true)
    URI url();

    /**
     * Whether debug logging is enabled.
     *
     * @return {@code true} if debug logging is enabled, {@code false} otherwise
     * @since 0.23.0
     */
    @Contract(pure = true)
    boolean debug();

    /**
     * A factory for creating {@link Settings} instances.
     *
     * @since 0.23.0
     */
    sealed interface Factory permits SimpleSettings.Factory {
        /**
         * Sets the token used to authenticate with the server and identify the project.
         * <p>
         * This token can be found in the settings of your project under <b>"Your API Token"</b>.
         *
         * @param token the token
         * @return the factory
         * @throws IllegalArgumentException if the token does not match the {@link Token#PATTERN}
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory token(@Token String token) throws IllegalArgumentException;

        /**
         * Sets the server URL.
         * <p>
         * This is only required for self-hosted servers.
         *
         * @param url the server URL
         * @return the factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory url(URI url);

        /**
         * Enables or disables debug logging.
         * <p>
         * This is only meant for development and testing and should not be enabled in production.
         *
         * @param enabled whether debug logging is enabled
         * @return the factory
         * @since 0.23.0
         */
        @Contract(mutates = "this")
        Factory debug(boolean enabled);

        /**
         * Creates a new {@link Settings} instance.
         *
         * @return the settings
         * @throws IllegalStateException if the token is not specified
         * @since 0.23.0
         */
        @Contract(value = " -> new", pure = true)
        Settings create() throws IllegalStateException;
    }
}
