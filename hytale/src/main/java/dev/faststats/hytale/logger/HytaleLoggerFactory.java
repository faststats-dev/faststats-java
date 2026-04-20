package dev.faststats.hytale.logger;

public final class HytaleLoggerFactory implements dev.faststats.internal.LoggerFactory {
    @Override
    public dev.faststats.internal.Logger getLogger(final String name) {
        return new HytaleLogger(name);
    }
}
