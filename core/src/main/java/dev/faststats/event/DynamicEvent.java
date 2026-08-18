package dev.faststats.event;

import com.google.gson.JsonElement;

import java.util.function.Supplier;

public class DynamicEvent implements Event {
    private final Key key;
    private final Supplier<JsonElement> supplier;

    public DynamicEvent(final Key key, final Supplier<JsonElement> supplier) {
        this.key = key;
        this.supplier = supplier;
    }

    @Override
    public JsonElement payload() {
        return supplier.get();
    }

    @Override
    public Key key() {
        return key;
    }
}
