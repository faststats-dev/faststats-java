package dev.faststats.hytale.logger;

import org.intellij.lang.annotations.PrintFormat;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
import java.util.logging.Level;

final class HytaleLogger implements dev.faststats.internal.Logger {
    private final com.hypixel.hytale.logger.HytaleLogger logger;
    private volatile @Nullable Predicate<Level> filter;

    HytaleLogger(final String name) {
        this.logger = com.hypixel.hytale.logger.HytaleLogger.get(name);
    }

    @Override
    public void setLevel(final Level level) {
        logger.setLevel(level);
    }

    @Override
    public boolean isLoggable(final Level level) {
        final var loggerLevel = logger.getLevel();
        if (level.intValue() < loggerLevel.intValue()) return false;

        final var currentFilter = filter;
        return currentFilter != null && currentFilter.test(level);
    }

    @Override
    public void setFilter(@Nullable final Predicate<Level> filter) {
        this.filter = filter;
    }

    @Override
    public void error(@PrintFormat final String message, @Nullable final Throwable throwable, @Nullable final Object... args) {
        if (!isLoggable(Level.SEVERE)) return;

        final var api = logger.atSevere();
        if (throwable != null) {
            api.withCause(throwable).logVarargs(message, args);
            return;
        }
        api.logVarargs(message, args);
    }

    @Override
    public void log(final Level level, @PrintFormat final String message, @Nullable final Object... args) {
        if (!isLoggable(level)) return;
        logger.at(level).logVarargs(message, args);
    }
}
