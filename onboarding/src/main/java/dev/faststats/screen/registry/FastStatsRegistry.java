package dev.faststats.screen.registry;

import dev.faststats.Config;
import dev.faststats.SimpleContext;
import dev.faststats.config.SimpleConfig;
import dev.faststats.data.Metric;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
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

    public synchronized void register(final SimpleContext context) {
        final var additionalMetrics = context.metrics().map(metrics -> {
            return metrics.stream()
                    .map(Metric::getId)
                    .collect(Collectors.toUnmodifiableSet());
        }).orElseGet(Set::of);

        contexts.put(context, new Entry(context, new FastStatsRegistration(
                context.getProjectName(),
                context.getSdkInfo().getName(),
                context.getSdkInfo().getVersion(),
                context.metrics().isPresent(),
                context.errorTrackerService().isPresent(),
                context.featureFlagService().isPresent(),
                additionalMetrics
        )));
    }

    public synchronized void unregister(final SimpleContext context) {
        contexts.remove(context);
    }

    public List<FastStatsRegistration> registrations() {
        return contexts.values().stream().map(Entry::registration).toList();
    }

    public Set<SimpleContext> contexts() {
        return contexts.keySet();
    }

    private synchronized List<Entry> entries() {
        return new ArrayList<>(contexts.values());
    }

    public boolean firstRun() {
        for (final var context : contexts.keySet()) {
            if (((SimpleConfig) context.getConfig()).firstRun()) return true;
        }
        return false;
    }

    public Config anyConfig() {
        for (final var context : contexts.keySet()) return context.getConfig();
        throw new IllegalStateException("Config not found!");
    }

    private record Entry(
            SimpleContext context,
            FastStatsRegistration registration
    ) {
    }
}
