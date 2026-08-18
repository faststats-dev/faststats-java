package dev.faststats.event;

import org.jetbrains.annotations.Contract;

public interface Key {
    @Contract(pure = true)
    String namespace();

    @Contract(pure = true)
    String value();

    @Contract(value = "_, _ -> new", pure = true)
    static Key key(final String namespace, final String value) {
        return new SimpleKey(namespace, value);
    }
}
