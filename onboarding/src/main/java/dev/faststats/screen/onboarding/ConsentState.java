package dev.faststats.screen.onboarding;

import dev.faststats.Config;
import dev.faststats.config.SimpleConfig;
import dev.faststats.screen.registry.FastStatsRegistry;

public final class ConsentState {
    private boolean submitMetrics;
    private boolean additionalMetrics;
    private boolean errorTracking;

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

    public synchronized void apply(final FastStatsRegistry registry) {
        registry.contexts().forEach(context -> {
            final var config = (SimpleConfig) context.getConfig();
            config.submitMetrics(submitMetrics);
            config.additionalMetrics(additionalMetrics());
            config.errorTracking(errorTracking);
            config.enabled(submitMetrics || errorTracking);
            config.persist();
            context.submissionActive = config.enabled();
        });
    }
}
