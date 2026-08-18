package dev.faststats.event;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

import java.util.function.Supplier;

public interface Event {
    @CheckReturnValue
    JsonElement payload();

    @Contract(pure = true)
    Key key();

    @Contract(value = "_, _ -> new", pure = true)
    static Event of(final Key key, final JsonElement payload) {
        return new SimpleEvent(key, payload);
    }

    @Contract(value = "_, _ -> new", pure = true)
    static Event dynamic(final Key key, final Supplier<JsonElement> supplier) {
        return new DynamicEvent(key, supplier);
    }
}
