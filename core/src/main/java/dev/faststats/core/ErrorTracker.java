package dev.faststats.core;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * An error tracker.
 *
 * @since 0.10.0
 */
public sealed interface ErrorTracker permits SimpleErrorTracker {
    /**
     * Create and attach a new context-aware error tracker.
     * <p>
     * This tracker will automatically track errors that occur in the same class loader as the tracker itself.
     * <p>
     * You can still manually track errors using {@code #trackError}.
     *
     * @return the error tracker
     * @see #contextUnaware()
     * @see #trackError(String)
     * @see #trackError(Throwable)
     * @since 0.10.0
     */
    @Contract(value = " -> new")
    static ErrorTracker contextAware() {
        final var tracker = new SimpleErrorTracker();
        tracker.attachErrorContext(ErrorTracker.class.getClassLoader());
        return tracker;
    }

    /**
     * Create a new context-unaware error tracker.
     * <p>
     * This tracker will not automatically track any errors.
     * <p>
     * You have to manually track errors using {@code #trackError}.
     *
     * @return the error tracker
     * @see #contextAware()
     * @see #trackError(String)
     * @see #trackError(Throwable)
     * @since 0.10.0
     */
    @Contract(value = " -> new")
    static ErrorTracker contextUnaware() {
        return new SimpleErrorTracker();
    }

    /**
     * Tracks an error.
     *
     * @param message the error message
     * @see #trackError(Throwable)
     * @since 0.10.0
     */
    @Contract(mutates = "this")
    void trackError(String message);

    /**
     * Tracks an error.
     *
     * @param error the error
     * @since 0.10.0
     */
    @Contract(mutates = "this")
    void trackError(Throwable error);

    /**
     * Attaches an error context to the tracker.
     * <p>
     * If the class loader is {@code null}, the tracker will track all errors.
     *
     * @param loader the class loader
     * @throws IllegalStateException if the error context is already attached
     * @since 0.10.0
     */
    void attachErrorContext(@Nullable ClassLoader loader) throws IllegalStateException;

    /**
     * Detaches the error context from the tracker.
     * <p>
     * This restores the original uncaught exception handler that was in place before
     * {@link #attachErrorContext(ClassLoader)} was called.
     * <p>
     * This should be called during shutdown to prevent {@link BootstrapMethodError}
     * when the provider's JAR file is closed.
     *
     * @since 0.13.0
     */
    void detachErrorContext();

    /**
     * Returns whether an error context is attached.
     *
     * @return whether an error context is attached
     * @since 0.13.0
     */
    boolean isContextAttached();

    /**
     * Sets the error event handler which will be called when an error is tracked automatically.
     * <p>
     * The purpose of this handler is to allow custom error handling like logging.
     *
     * @param errorEvent the error event handler
     * @since 0.11.0
     */
    @Contract(mutates = "this")
    void setContextErrorHandler(@Nullable BiConsumer<@Nullable ClassLoader, Throwable> errorEvent);

    /**
     * Returns the error event handler which will be called when an error is tracked automatically.
     *
     * @return the error event handler
     * @since 0.11.0
     */
    @Contract(pure = true)
    Optional<BiConsumer<@Nullable ClassLoader, Throwable>> getContextErrorHandler();

    /**
     * Checks if the error occurred in the same class loader as the provided loader.
     *
     * @param loader the class loader
     * @param error  the error
     * @return whether the error occurred in the same class loader
     * @since 0.14.0
     */
    @Contract(pure = true)
    static boolean isSameLoader(final ClassLoader loader, final Throwable error) {
        return ErrorHelper.isSameLoader(loader, error);
    }
}    
