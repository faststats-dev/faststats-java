package dev.faststats;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Set;

/**
 * JFR performance collection options.
 *
 * @since 0.27.0
 */
public sealed interface JfrPerformanceOptions permits SimpleJfrPerformanceOptions {
    /**
     * Creates a JFR performance options factory.
     *
     * @return the factory
     * @since 0.27.0
     */
    @Contract(value = " -> new", pure = true)
    static Factory factory() {
        return new SimpleJfrPerformanceOptions.Factory();
    }

    /**
     * Gets the maximum retained event age while a recorder is running.
     *
     * @return the recording duration
     * @since 0.27.0
     */
    @Contract(pure = true)
    Duration duration();

    /**
     * Gets the JFR event names to enable and summarize.
     *
     * @return the enabled event names
     * @since 0.27.0
     */
    @Contract(pure = true)
    Set<String> eventNames();

    /**
     * Gets the maximum number of events to read from one recording.
     *
     * @return the maximum event count
     * @since 0.27.0
     */
    @Contract(pure = true)
    int maxEvents();

    /**
     * Gets the optional class loader used for stack-frame attribution.
     *
     * @return the attribution class loader
     * @since 0.27.0
     */
    @Contract(pure = true)
    @Nullable ClassLoader attributionLoader();

    /**
     * Creates a factory pre-populated with this options instance.
     *
     * @return the pre-populated factory
     * @since 0.27.0
     */
    @Contract(value = " -> new", pure = true)
    Factory toFactory();

    /**
     * JFR performance options factory.
     *
     * @since 0.27.0
     */
    sealed interface Factory permits SimpleJfrPerformanceOptions.Factory {
        /**
         * Configures the maximum retained event age while a recorder is running.
         *
         * @param duration the recording duration
         * @return this factory
         * @since 0.27.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory duration(Duration duration);

        /**
         * Configures the JFR event names to enable and summarize.
         *
         * @param eventNames the enabled event names
         * @return this factory
         * @since 0.27.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory eventNames(Set<String> eventNames);

        /**
         * Configures the maximum number of events to read from one recording.
         *
         * @param maxEvents the maximum event count
         * @return this factory
         * @since 0.27.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory maxEvents(int maxEvents);

        /**
         * Configures the optional class loader used for stack-frame attribution.
         *
         * @param loader the attribution class loader
         * @return this factory
         * @since 0.27.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory attributionLoader(@Nullable ClassLoader loader);

        /**
         * Creates the JFR performance options.
         *
         * @return the JFR performance options
         * @since 0.27.0
         */
        @Contract(value = " -> new", pure = true)
        JfrPerformanceOptions create();
    }
}
