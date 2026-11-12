package dev.faststats.screen.registry;

import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import dev.faststats.data.Metric;
import org.jetbrains.annotations.Contract;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Registers a context for the consent screen.
     *
     * @param context         the context
     * @param consentRequired whether the context requires the consent screen to be shown before submitting
     */
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
        ), (SimpleConfig) context.getConfig(), consentRequired));
    }

    public synchronized void unregister(final FastStatsContext context) {
        contexts.remove((SimpleContext) context);
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
        for (final var entry : contexts.values()) {
            if (entry.consentRequired() && !entry.config().consentScreenShown()) return true;
        }
        return false;
    }

    public synchronized SimpleConfig anyConfig() throws IllegalStateException {
        for (final var entry : contexts.values()) return entry.config();
        throw new IllegalStateException("Config not found!");
    }

    /**
     * Applies the given consent to every registered context.
     * <p>
     * Each context gets an updated config, and submission is started if anything was allowed
     * or stopped otherwise.
     *
     * @param submitMetrics     whether metrics submission was allowed
     * @param additionalMetrics whether additional metrics were allowed
     * @param errorTracking     whether error tracking was allowed
     * @return the updated config to persist, if any context is registered
     */
    public synchronized Optional<SimpleConfig> applyConsent(final boolean submitMetrics, final boolean additionalMetrics, final boolean errorTracking) {
        SimpleConfig updated = null;
        for (final var registered : contexts.entrySet()) {
            final var context = registered.getKey();
            final var entry = registered.getValue();
            final var config = entry.config().withConsent(submitMetrics, additionalMetrics, errorTracking);
            registered.setValue(new Entry(entry.registration(), config, entry.consentRequired()));
            context.updateConfig(config);
            if (config.enabled()) context.startSubmission();
            else context.stopSubmission();
            if (updated == null) updated = config;
        }
        return Optional.ofNullable(updated);
    }

    private record Entry(FastStatsRegistration registration, SimpleConfig config, boolean consentRequired) {
    }
}
