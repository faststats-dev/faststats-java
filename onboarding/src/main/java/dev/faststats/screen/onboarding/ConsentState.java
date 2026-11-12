package dev.faststats.screen.onboarding;

import dev.faststats.Config;
import dev.faststats.config.CustomConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.screen.registry.FastStatsRegistry;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ConsentState {
    public static final CustomConfig.Field<Boolean> CONSENT_SCREEN_SHOWN = CustomConfig.Field.bool("consent-screen-shown", false);

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

    public static CustomConfig readConfig(final Path file, final LoggerFactory loggerFactory, final boolean client) {
        final var builder = CustomConfig.builder(file, loggerFactory);
        if (client) builder.field(CONSENT_SCREEN_SHOWN);
        return builder.build();
    }

    public static boolean consentScreenShown(final CustomConfig config) {
        return config.has(CONSENT_SCREEN_SHOWN) && config.get(CONSENT_SCREEN_SHOWN);
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
        return CustomConfig.enabledFlag();
    }

    public synchronized void apply(final FastStatsRegistry registry) {
        if (!editable()) return;

        final var contexts = registry.contexts();
        if (contexts.isEmpty()) return;
        final var logger = contexts.get(0).getLoggerFactory().getLogger(ConsentState.class);

        registry.applyConsent(submitMetrics, additionalMetrics(), errorTracking).ifPresent(config -> {
            PERSISTENCE.execute(() -> {
                try {
                    config.persist();
                } catch (final Throwable t) {
                    logger.error("Failed to save FastStats consent", t);
                }
            });
        });
    }
}
