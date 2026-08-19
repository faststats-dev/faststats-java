package dev.faststats.internal;

import org.intellij.lang.annotations.PrintFormat;
import org.jspecify.annotations.Nullable;

import java.util.logging.Level;

public sealed interface Logger permits PlatformLoggerFactory.PlatformLogger {
    default void error(@PrintFormat final String message, @Nullable final Throwable t, final Object... args) {
        debug(LogLevel.ERROR, message, t, args);
    }

    default void info(@PrintFormat final String message, final Object... args) {
        debug(LogLevel.INFO, message, null, args);
    }

    default void warn(@PrintFormat final String message, final Object... args) {
        debug(LogLevel.WARN, message, null, args);
    }

    default void debug(final LogLevel level, @PrintFormat final String message, @Nullable final Throwable t, final Object... args) {
        if (factory().isDebug()) print(level, t, "[" + caller() + "] " + message.formatted(args));
    }

    String caller();

    LoggerFactory factory();

    void print(LogLevel level, @Nullable Throwable t, String message);

    enum LogLevel {
        ERROR(Level.SEVERE),
        INFO(Level.INFO),
        WARN(Level.WARNING);
        private final Level level;

        LogLevel(final Level level) {
            this.level = level;
        }

        public Level getLevel() {
            return level;
        }
    }
}
