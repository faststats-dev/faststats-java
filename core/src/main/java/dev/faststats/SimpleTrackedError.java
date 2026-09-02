package dev.faststats;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

final class SimpleTrackedError implements TrackedError {
    private volatile Attributes attributes = Attributes.empty();
    private volatile boolean handled = true;
    private final ThrowableSnapshot error;

    SimpleTrackedError(final Throwable error) {
        this.error = snapshot(error, null);
    }

    @Contract("_, null -> !null")
    private static @Nullable ThrowableSnapshot snapshot(final Throwable error, @Nullable Set<Throwable> visited) {
        final var message = error.getMessage();
        final var stackTrace = error.getStackTrace();
        if (error.getCause() != null && visited == null)
            visited = Collections.newSetFromMap(new IdentityHashMap<>());
        if (visited != null && !visited.add(error)) return null;
        final var cause = error.getCause() != null
                ? snapshot(error.getCause(), visited)
                : null;
        final var trace = stackTrace.length == 0 ? new Throwable().getStackTrace() : stackTrace;
        return new SimpleThrowableSnapshot(error.getClass(), message, cause, trace);
    }

    record SimpleThrowableSnapshot(
            Class<?> type,
            @Nullable String message,
            @Nullable ThrowableSnapshot cause,
            StackTraceElement... stackTraces
    ) implements ThrowableSnapshot {
        @Override
        public StackTraceElement[] stackTraces() {
            return stackTraces.clone();
        }
        
        @Override
        public boolean equals(@Nullable final Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            final SimpleThrowableSnapshot that = (SimpleThrowableSnapshot) o;
            return Objects.equals(type, that.type)
                    && Objects.equals(message, that.message)
                    && Objects.equals(cause, that.cause)
                    && Objects.deepEquals(stackTraces, that.stackTraces);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, message, cause, Arrays.hashCode(stackTraces));
        }
    }

    @Override
    public ThrowableSnapshot error() {
        return error;
    }

    @Override
    public boolean handled() {
        return handled;
    }

    @Override
    public TrackedError handled(final boolean handled) {
        this.handled = handled;
        return this;
    }

    @Override
    public Attributes attributes() {
        return Attributes.copyOf(attributes);
    }

    @Override
    public TrackedError attributes(final Attributes attributes) {
        this.attributes = Attributes.copyOf(attributes);
        return this;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final SimpleTrackedError that = (SimpleTrackedError) o;
        return handled == that.handled
                && Objects.equals(attributes, that.attributes)
                && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, handled, error);
    }

    @Override
    public String toString() {
        return "SimpleTrackedError{" +
                "attributes=" + attributes +
                ", handled=" + handled +
                ", error=" + error +
                '}';
    }
}
