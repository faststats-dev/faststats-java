package dev.faststats.core.internal;

import org.jspecify.annotations.Nullable;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class SimpleLogger implements Logger {
    private final java.util.logging.Logger logger;

    public SimpleLogger(final String name) {
        this.logger = java.util.logging.Logger.getLogger(name);
    }

    @Override
    public void setLevel(final Level level) {
        logger.setLevel(level);
    }

    @Override
    public boolean isLoggable(final Level level) {
        return logger.isLoggable(level);
    }

    @Override
    public void setFilter(@Nullable final Filter filter) {
        logger.setFilter(filter);
    }

    @Override
    public void error(final String message, @Nullable final Throwable throwable, @Nullable final Object... args) {
        if (throwable != null) {
            if (!logger.isLoggable(Level.SEVERE)) return;
            final var logRecord = new LogRecord(Level.SEVERE, message.formatted(args));
            logRecord.setThrown(throwable);
            logger.log(logRecord);
        } else log(Level.SEVERE, message, args);
    }

    @Override
    public void log(final Level level, final String message, @Nullable final Object... args) {
        logger.log(level, () -> message.formatted(args));
    }
}
