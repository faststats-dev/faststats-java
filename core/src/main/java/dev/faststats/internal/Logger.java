package dev.faststats.internal;

import org.intellij.lang.annotations.PrintFormat;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
import java.util.logging.Level;

public interface Logger {
    void setLevel(Level level);

    boolean isLoggable(Level level);

    void setFilter(@Nullable Predicate<Level> filter);

    void error(@PrintFormat final String message, @Nullable final Throwable throwable, @Nullable final Object... args);

    void log(final Level level, @PrintFormat final String message, @Nullable final Object... args);

    default void info(@PrintFormat final String message, @Nullable final Object... args) {
        log(Level.INFO, message, args);
    }

    default void warn(@PrintFormat final String message, @Nullable final Object... args) {
        log(Level.WARNING, message, args);
    }
}
