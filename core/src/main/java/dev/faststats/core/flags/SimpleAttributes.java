package dev.faststats.core.flags;

import com.google.gson.JsonPrimitive;

import java.util.Map;
import java.util.function.BiConsumer;

record SimpleAttributes(Map<String, JsonPrimitive> attributes) implements Attributes {
    @Override
    public Attributes put(final String key, final String value) {
        attributes.put(key, new JsonPrimitive(value));
        return this;
    }

    @Override
    public Attributes put(final String key, final Number value) {
        if (!Double.isFinite(value.doubleValue())) throw new IllegalArgumentException("Value must be finite");
        attributes.put(key, new JsonPrimitive(value));
        return this;
    }

    @Override
    public Attributes put(final String key, final boolean value) {
        attributes.put(key, new JsonPrimitive(value));
        return this;
    }

    @Override
    public Attributes remove(final String key) {
        attributes.remove(key);
        return this;
    }

    @Override
    public void forEachPrimitive(final BiConsumer<String, JsonPrimitive> action) {
        attributes.forEach(action);
    }
}
