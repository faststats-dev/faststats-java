package dev.faststats;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registry of contexts that opt into live onboarding lifecycle updates.
 *
 * @since 0.29.2
 */
public final class FastStatsRegistry {
    private static final FastStatsRegistry INSTANCE = new FastStatsRegistry();

    private final Map<SimpleContext, Entry> contexts = new IdentityHashMap<>();
    private @Nullable ConfigSource configSource;

    private FastStatsRegistry() {
    }

    @Contract(pure = true)
    public static FastStatsRegistry instance() {
        return INSTANCE;
    }

    /** Registers a context and the shared configuration lifecycle used by its platform. */
    public synchronized void register(
            final SimpleContext context,
            final Supplier<? extends Config> configLoader,
            final Consumer<SubmissionSettings> configUpdater
    ) {
        if (configSource == null) configSource = new ConfigSource(configLoader, configUpdater);
        contexts.put(context, new Entry(context, context.registration()));
    }

    /** Removes a context when its platform lifecycle ends. */
    public synchronized void unregister(final SimpleContext context) {
        contexts.remove(context);
        if (contexts.isEmpty()) configSource = null;
    }

    /** Returns the latest configuration from the registered platform. */
    public synchronized Config config() {
        return loadConfig();
    }

    /** Returns a snapshot of every registered FastStats consumer. */
    public synchronized List<FastStatsRegistration> registrations() {
        return contexts.values().stream().map(Entry::registration).toList();
    }

    /** Persists settings through the registered platform configuration source. */
    public void updateSubmissionSettings(final SubmissionSettings settings) {
        final Consumer<SubmissionSettings> updater;
        synchronized (this) {
            updater = configSource().updater();
        }
        updater.accept(settings);
    }

    /** Reloads the immutable config snapshot and enables submission for all contexts. */
    public void start() {
        final var config = loadConfig();
        for (final var entry : entries()) {
            entry.context().startSubmissions(config);
        }
    }

    /** Reloads the immutable config snapshot and disables submission for all contexts. */
    public void shutdown() {
        final var config = loadConfig();
        for (final var entry : entries()) {
            entry.context().stopSubmissions(config);
        }
    }

    private synchronized List<Entry> entries() {
        return new ArrayList<>(contexts.values());
    }

    private synchronized Config loadConfig() {
        return configSource().loader().get();
    }

    private ConfigSource configSource() {
        final var source = configSource;
        if (source == null) throw new IllegalStateException("No FastStats contexts are registered");
        return source;
    }

    private record Entry(
            SimpleContext context,
            FastStatsRegistration registration
    ) {
    }

    private record ConfigSource(
            Supplier<? extends Config> loader,
            Consumer<SubmissionSettings> updater
    ) {
    }
}
