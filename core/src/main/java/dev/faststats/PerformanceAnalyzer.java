package dev.faststats;

import dev.faststats.SimplePerformanceAnalyzer.NoopFlightRecorder;
import dev.faststats.SimplePerformanceAnalyzer.SimpleFlightRecorder;
import jdk.jfr.Recording;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.Callable;

/**
 * Runtime performance analyzer.
 *
 * @since 0.26.0
 */
public sealed interface PerformanceAnalyzer permits SimplePerformanceAnalyzer {
    /**
     * Wraps a data source so SQL calls made through acquired connections are timed.
     *
     * @param dataSource the source to wrap
     * @return the wrapped data source
     * @since 0.26.0
     */
    @Contract(value = "_ -> new", pure = true)
    DataSource wrap(DataSource dataSource);

    /**
     * Wraps a connection so SQL calls made through it are timed.
     *
     * @param connection the connection to wrap
     * @return the wrapped connection
     * @since 0.26.0
     */
    @Contract(value = "_ -> new", pure = true)
    Connection wrap(Connection connection);

    /**
     * Starts a non-blocking JFR recording.
     * <p>
     * The returned recorder owns the recording and dumps/summarizes it when stopped.
     *
     * @return the started recorder
     * @since 0.26.0
     */
    @CheckReturnValue
    FlightRecorder recordJfrSnapshot();

    /**
     * Records the time taken by a runnable.
     *
     * @param name     the recording name
     * @param runnable the runnable to time
     * @since 0.26.0
     */
    void record(String name, Runnable runnable);

    /**
     * Records the time taken by a callable and returns its value.
     *
     * @param name     the recording name
     * @param callable the callable to time
     * @param <T>      the return type
     * @return the callable result
     * @throws Exception if the callable fails
     * @since 0.26.0
     */
    <T> T record(String name, Callable<T> callable) throws Exception;

    /**
     * Starts a manual time recording that the caller stops.
     *
     * @param name the recording name
     * @return the started time recording
     * @since 0.26.0
     */
    @CheckReturnValue
    TimeRecording startRecording(String name);

    /**
     * Uploads the currently collected performance data.
     *
     * @return {@code true} if the upload succeeded
     * @since 0.26.0
     */
    boolean flush();

    /**
     * Shuts the analyzer down and performs a best-effort final upload.
     *
     * @since 0.26.0
     */
    void shutdown();

    /**
     * Performance analyzer factory.
     *
     * @since 0.26.0
     */
    sealed interface Factory permits SimplePerformanceAnalyzer.Factory {
        /**
         * Configures SQL collection.
         *
         * @param options SQL options
         * @return this factory
         * @since 0.26.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory sql(SqlPerformanceOptions options);

        /**
         * Configures JFR collection.
         *
         * @param options JFR options
         * @return this factory
         * @since 0.26.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        Factory jfr(JfrPerformanceOptions options);

        /**
         * Creates the analyzer.
         *
         * @return the analyzer
         * @since 0.26.0
         */
        @Contract(value = " -> new", pure = true)
        PerformanceAnalyzer create();
    }

    /**
     * A running JFR recorder.
     *
     * @since 0.26.0
     */
    sealed interface FlightRecorder extends AutoCloseable permits NoopFlightRecorder, SimpleFlightRecorder {
        /**
         * Gets the underlying JFR recording for caller mutation.
         *
         * @return the running recording
         * @since 0.26.0
         */
        @Contract(pure = true)
        Recording recording();

        /**
         * Gets whether this recorder has already been stopped.
         *
         * @return {@code true} if stopped
         * @since 0.26.0
         */
        @Contract(pure = true)
        boolean isClosed();

        /**
         * Stops this recording.
         *
         * @since 0.26.0
         */
        @Override
        void close();
    }

    /**
     * A manually controlled time recording.
     *
     * @since 0.26.0
     */
    sealed interface TimeRecording extends AutoCloseable permits SimplePerformanceAnalyzer.SimpleTimeRecording {
        /**
         * Stops this recording as failed and stores its duration.
         *
         * @since 0.26.0
         */
        void fail();

        /**
         * Gets whether this recording has already been stopped.
         *
         * @return {@code true} if stopped
         * @since 0.26.0
         */
        @Contract(pure = true)
        boolean isClosed();

        /**
         * Stops this recording.
         *
         * @since 0.26.0
         */
        @Override
        void close();
    }
}
