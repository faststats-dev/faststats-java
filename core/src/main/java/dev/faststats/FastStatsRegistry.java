package dev.faststats;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of contexts that opt into live onboarding lifecycle updates.
 *
 * @since 0.29.2
 */
public final class FastStatsRegistry {
    private static final FastStatsRegistry INSTANCE = new FastStatsRegistry();

    private final Map<SimpleContext, Entry> contexts = new IdentityHashMap<>();
    private @Nullable Config config;

    private FastStatsRegistry() {
    }

    @Contract(pure = true)
    public static FastStatsRegistry instance() {
        return INSTANCE;
    }

    /** Registers a context and the shared mutable configuration used by its platform. */
    public synchronized void register(final SimpleContext context) {
        if (this.config == null) this.config = context.getConfig();
        contexts.put(context, new Entry(context, context.registration()));
    }

    /** Removes a context when its platform lifecycle ends. */
    public synchronized void unregister(final SimpleContext context) {
        contexts.remove(context);
        if (contexts.isEmpty()) config = null;
    }

    /** Returns a snapshot of every registered FastStats consumer. */
    public synchronized List<FastStatsRegistration> registrations() {
        return contexts.values().stream().map(Entry::registration).toList();
    }

    /** Enables submission for all contexts using the shared configuration. */
    public void start() {
        final var config = config();
        for (final var entry : entries()) {
            entry.context().startSubmissions(config);
        }
    }

    /** Disables submission for all contexts using the shared configuration. */
    public void shutdown() {
        final var config = config();
        for (final var entry : entries()) {
            entry.context().stopSubmissions(config);
        }
    }

    private synchronized List<Entry> entries() {
        return new ArrayList<>(contexts.values());
    }

    public synchronized Config config() {
        final var config = this.config;
        if (config == null) throw new IllegalStateException("No FastStats contexts are registered");
        return config;
    }

    private record Entry(
            SimpleContext context,
            FastStatsRegistration registration
    ) {
    }
}
