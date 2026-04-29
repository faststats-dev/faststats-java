package dev.faststats.internal;

final class SimpleLoggerFactory implements LoggerFactory {
    @Override
    public Logger getLogger(final String name) {
        return new SimpleLogger(name);
    }
}
