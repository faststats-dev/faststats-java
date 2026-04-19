package dev.faststats.hytale.logger;

public final class HytaleLoggerFactory implements dev.faststats.core.internal.LoggerFactory {
    @Override
    public dev.faststats.core.internal.Logger getLogger(final String name) {
        return new HytaleLogger(name);
    }
}
