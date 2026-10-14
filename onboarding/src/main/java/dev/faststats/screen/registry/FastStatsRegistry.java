package dev.faststats.screen.registry;

import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import dev.faststats.data.Metric;
import org.jetbrains.annotations.Contract;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class FastStatsRegistry {
    private static final FastStatsRegistry INSTANCE = new FastStatsRegistry();

    private final Map<SimpleContext, Entry> contexts = new IdentityHashMap<>();

    private FastStatsRegistry() {
    }

    @Contract(pure = true)
    public static FastStatsRegistry instance() {
        return INSTANCE;
    }

    public synchronized void register(final SimpleContext context, final boolean consentRequired) {
        final var additionalMetrics = context.metrics()
                .filter(SimpleMetrics.class::isInstance)
                .map(metrics -> ((SimpleMetrics) metrics).stream()
                        .map(Metric::getId)
                        .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);

        contexts.put(context, new Entry(new FastStatsRegistration(
                context.getProjectName(),
                context.metrics().isPresent(),
                context.errorTrackerService().isPresent(),
                additionalMetrics
        ), consentRequired));
    }

    public synchronized void unregister(final SimpleContext context) {
        contexts.remove(context);
    }

    public synchronized boolean isEmpty() {
        return contexts.isEmpty();
    }

    public synchronized List<FastStatsRegistration> registrations() {
        return contexts.values().stream().map(Entry::registration).toList();
    }

    public synchronized List<SimpleContext> contexts() {
        return List.copyOf(contexts.keySet());
    }

    public synchronized boolean consentPending() {
        for (final var entry : contexts.entrySet()) {
            if (!entry.getValue().consentRequired()) continue;
            if (!((SimpleConfig) entry.getKey().getConfig()).consentScreenShown()) return true;
        }
        return false;
    }

    public synchronized SimpleConfig anyConfig() throws IllegalStateException {
        for (final var context : contexts.keySet()) return (SimpleConfig) context.getConfig();
        throw new IllegalStateException("Config not found!");
    }

    private record Entry(FastStatsRegistration registration, boolean consentRequired) {
    }
}
