package dev.faststats.core.flags;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class SimpleFeatureFlag<T> implements FeatureFlag<T> {
    private final SimpleFeatureFlagService service;

    private final String id;
    private final T defaultValue;
    private final @Nullable Attributes attributes;
    private final Type type;

    SimpleFeatureFlag(
            final String id,
            final T defaultValue,
            final @Nullable Attributes attributes,
            final SimpleFeatureFlagService service
    ) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.attributes = attributes;
        this.service = service;
        if (defaultValue instanceof final String string) {
            this.type = Type.STRING;
        } else if (defaultValue instanceof final Number number) {
            this.type = Type.NUMBER;
        } else if (defaultValue instanceof final Boolean bool) {
            this.type = Type.BOOLEAN;
        } else throw new IllegalArgumentException("Unsupported type: " + defaultValue.getClass().getName());
        service.fetch(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getTypeClass() {
        return (Class<T>) switch (type) {
            case STRING -> String.class;
            case NUMBER -> Number.class;
            case BOOLEAN -> Boolean.class;
        };
    }

    @Override
    public Optional<T> getCached() {
        return service.get(this);
    }

    @Override
    public Optional<Instant> getExpiration() {
        return service.getExpiration(this);
    }

    @Override
    public boolean isValid() {
        return service.isValid(this);
    }

    @Override
    public CompletableFuture<T> whenReady() {
        return service.whenReady(this);
    }

    @Override
    public CompletableFuture<T> fetch() {
        return service.fetch(this);
    }

    @Override
    public CompletableFuture<T> optIn() {
        return service.optIn(this);
    }

    @Override
    public CompletableFuture<T> optOut() {
        return service.optOut(this);
    }

    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    @Nullable Attributes attributes() {
        return attributes;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final SimpleFeatureFlag<?> that = (SimpleFeatureFlag<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SimpleFeatureFlag{" +
                "id='" + id + '\'' +
                '}';
    }
}
