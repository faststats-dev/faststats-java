package dev.faststats.screen.onboarding;

import dev.faststats.Config;
import dev.faststats.config.SimpleConfig;
import dev.faststats.screen.registry.FastStatsRegistry;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ConsentState {
    private static final Executor PERSISTENCE = Executors.newSingleThreadExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-config-writer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean submitMetrics;
    private volatile boolean additionalMetrics;
    private volatile boolean errorTracking;

    public ConsentState(final Config config) {
        this(config.submitMetrics(), config.additionalMetrics(), config.errorTracking());
    }

    ConsentState(final boolean submitMetrics, final boolean additionalMetrics, final boolean errorTracking) {
        this.submitMetrics = submitMetrics;
        this.additionalMetrics = submitMetrics && additionalMetrics;
        this.errorTracking = errorTracking;
    }

    public boolean submitMetrics() {
        return submitMetrics;
    }

    public void submitMetrics(final boolean enabled) {
        submitMetrics = enabled;
        if (!enabled) additionalMetrics = false;
    }

    public boolean additionalMetrics() {
        return submitMetrics && additionalMetrics;
    }

    public void additionalMetrics(final boolean enabled) {
        additionalMetrics = submitMetrics && enabled;
    }

    public boolean errorTracking() {
        return errorTracking;
    }

    public void errorTracking(final boolean enabled) {
        errorTracking = enabled;
    }

    public void selectAll(final boolean selected) {
        submitMetrics = selected;
        additionalMetrics = selected;
        errorTracking = selected;
    }

    public boolean editable() {
        return SimpleConfig.enabledFlag();
    }

    public synchronized void apply(final FastStatsRegistry registry) {
        if (!editable()) return;

        final var contexts = registry.contexts();
        if (contexts.isEmpty()) return;

        final var enabled = submitMetrics || errorTracking;
        contexts.forEach(context -> {
            final var config = (SimpleConfig) context.getConfig();
            config.submitMetrics(submitMetrics);
            config.additionalMetrics(additionalMetrics());
            config.errorTracking(errorTracking);
            config.enabled(enabled);
            config.consentScreenShown(true);
            context.submissionActive = enabled;
        });

        final var context = contexts.get(0);
        final var config = (SimpleConfig) context.getConfig();
        PERSISTENCE.execute(() -> {
            try {
                config.persist();
            } catch (final Throwable t) {
                context.getLoggerFactory().getLogger(ConsentState.class).error("Failed to save FastStats consent", t);
            }
        });
    }
}
