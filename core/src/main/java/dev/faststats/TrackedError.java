package dev.faststats;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * An error report with tracking metadata.
 *
 * @since 0.24.0
 */
public sealed interface TrackedError permits SimpleTrackedError {
    /**
     * Returns a snapshot of the tracked error.
     *
     * @return a snapshot of the tracked error
     * @since 0.30.0
     */
    @Contract(pure = true)
    ThrowableSnapshot error();

    /**
     * A snapshot of a {@link Throwable} captured when an error is tracked.
     * <p>
     * The snapshot preserves the throwable's type, message, cause chain, and stack trace
     * without retaining the original throwable.
     *
     * @since 0.30.0
     */
    sealed interface ThrowableSnapshot permits SimpleTrackedError.SimpleThrowableSnapshot {
        /**
         * Returns the throwable class.
         *
         * @return the throwable class
         */
        Class<?> type();

        /**
         * Returns the throwable message.
         *
         * @return the throwable message, or {@code null} if none was provided
         */
        @Nullable String message();

        /**
         * Returns a snapshot of the throwable's cause.
         *
         * @return a snapshot of the throwable's cause, or {@code null} if it has no cause
         */
        @Nullable ThrowableSnapshot cause();

        /**
         * Returns a copy of the throwable's stack trace elements.
         *
         * @return a copy of the throwable's stack trace elements
         */
        StackTraceElement[] stackTraces();
    }

    /**
     * Returns whether the error was handled.
     *
     * @return whether the error was handled
     * @since 0.24.0
     */
    @Contract(pure = true)
    boolean handled();

    /**
     * Sets whether the error was handled.
     *
     * @param handled whether the error was handled
     * @return this tracked error
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    TrackedError handled(boolean handled);

    /**
     * Returns a copy of the additional error attributes.
     *
     * @return a copy of the additional error attributes
     * @since 0.24.0
     */
    @Contract(value = " -> new", pure = true)
    Attributes attributes();

    /**
     * Sets the additional error attributes.
     *
     * @param attributes the additional error attributes
     * @return this tracked error
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    TrackedError attributes(Attributes attributes);
}
