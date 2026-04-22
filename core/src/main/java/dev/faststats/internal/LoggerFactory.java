package dev.faststats.internal;

import org.jetbrains.annotations.Contract;

import java.util.ServiceLoader;

public interface LoggerFactory {
    @Contract(pure = true)
    static LoggerFactory factory() {
        final class Holder {
            private static final LoggerFactory INSTANCE = ServiceLoader.load(LoggerFactory.class)
                    .findFirst()
                    .orElseGet(SimpleLoggerFactory::new);
        }
        return Holder.INSTANCE;
    }

    @Contract(value = "_ -> new", pure = true)
    default Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    @Contract(value = "_ -> new", pure = true)
    Logger getLogger(String name);
}
