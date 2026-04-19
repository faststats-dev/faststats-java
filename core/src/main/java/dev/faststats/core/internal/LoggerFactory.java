package dev.faststats.core.internal;

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

    Logger getLogger(String name);
}
