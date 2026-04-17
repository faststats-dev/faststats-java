package dev.faststats.core.flags;

import java.util.Map;

record SimpleAttributes(Map<String, Object> attributes) implements Attributes {
    @Override
    public Attributes put(final String key, final String value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public Attributes put(final String key, final Number value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public Attributes put(final String key, final boolean value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public Attributes remove(final String key) {
        attributes.remove(key);
        return this;
    }

    @Override
    public Map<String, Object> entries() {
        return Map.copyOf(attributes);
    }
}
