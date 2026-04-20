package dev.faststats.internal;

import java.util.ServiceLoader;

public interface LoggerFactory {
    static LoggerFactory factory() {
        final class Holder {
            private static final LoggerFactory INSTANCE = ServiceLoader.load(LoggerFactory.class)
                    .findFirst()
                    .orElseGet(SimpleLoggerFactory::new);
        }
        return Holder.INSTANCE;
    }

    default Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    Logger getLogger(String name);
}
