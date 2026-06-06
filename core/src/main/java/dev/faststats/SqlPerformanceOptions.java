package dev.faststats;

import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Contract;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL performance collection options.
 *
 * @since 0.26.0
 */
public sealed interface SqlPerformanceOptions permits SimpleSqlPerformanceOptions {
    /**
     * Creates a SQL performance options factory.
     *
     * @return the factory
     * @since 0.26.0
     */
    @Contract(value = " -> new", pure = true)
    static Factory factory() {
        return new SimpleSqlPerformanceOptions.Factory();
    }

    /**
     * Gets the query duration at or above which a call is counted as slow.
     *
     * @return the slow query threshold
     * @since 0.26.0
     */
    @Contract(pure = true)
    Duration slowQueryThreshold();

    /**
     * Gets the maximum number of distinct sanitized query shapes to track.
     *
     * @return the maximum query shape count
     * @since 0.26.0
     */
    @Contract(pure = true)
    int maxQueryShapes();

    /**
     * Gets whether aggregate JDBC update counts are included.
     *
     * @return {@code true} if update counts are included
     * @since 0.26.0
     */
    @Contract(pure = true)
    boolean includeUpdateCounts();

    /**
     * Gets the SQL anonymization patterns applied to recorded query shapes.
     *
     * @return SQL anonymization patterns
     * @since 0.26.0
     */
    @Contract(pure = true)
    List<Map.Entry<Pattern, String>> anonymizationEntries();

    /**
     * Creates a factory pre-populated with this options instance.
     *
     * @return the pre-populated factory
     * @since 0.26.0
     */
    @Contract(value = " -> new", pure = true)
    Factory toFactory();

    /**
     * SQL performance options factory.
     *
     * @since 0.26.0
     */
    sealed interface Factory permits SimpleSqlPerformanceOptions.Factory {
        /**
         * Configures the query duration at or above which a call is counted as slow.
         *
         * @param threshold the slow query threshold
         * @return this factory
         * @since 0.26.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory slowQueryThreshold(Duration threshold);

        /**
         * Configures the maximum number of distinct sanitized query shapes to track.
         *
         * @param maxQueryShapes the maximum query shape count
         * @return this factory
         * @since 0.26.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory maxQueryShapes(int maxQueryShapes);

        /**
         * Configures whether aggregate JDBC update counts are included.
         *
         * @param includeUpdateCounts {@code true} to include update counts
         * @return this factory
         * @since 0.26.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory includeUpdateCounts(boolean includeUpdateCounts);

        /**
         * Adds a SQL anonymization pattern that replaces matched text in recorded query shapes.
         *
         * @param pattern     pattern to replace
         * @param replacement replacement string
         * @return this factory
         * @since 0.26.0
         */
        @Contract(value = "_, _ -> this", mutates = "this")
        Factory anonymize(Pattern pattern, String replacement);

        /**
         * Adds a SQL anonymization pattern that replaces matched text in recorded query shapes.
         *
         * @param pattern     pattern to replace
         * @param replacement replacement string
         * @return this factory
         * @since 0.26.0
         * @see #anonymize(Pattern, String)
         */
        @Contract(value = "_, _ -> this", mutates = "this")
        default Factory anonymize(@RegExp final String pattern, final String replacement) {
            return anonymize(Pattern.compile(pattern), replacement);
        }

        /**
         * Creates the SQL performance options.
         *
         * @return the SQL performance options
         * @since 0.26.0
         */
        @Contract(value = " -> new", pure = true)
        SqlPerformanceOptions create();
    }
}
