package dev.faststats.forge;

import dev.faststats.screen.registry.FastStatsRegistry;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import dev.faststats.forge.compat.CompatibilityLayer;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Forge FastStats context.
 *
 * @since 0.31.0
 */
public final class ForgeContext extends SimpleContext {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();
    private final CompatibilityLayer compatibilityLayer;
    private final String modId;
    private final String modVersion;
    private final boolean consentRequired;

    private ForgeContext(final Factory factory, final LoggerFactory loggerFactory, final String modId, @Token final String token) {
        this(factory, loggerFactory, modId, token, ServiceLoader.load(CompatibilityLayer.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Forge compatibility layer found")));
    }

    private ForgeContext(final Factory factory, final LoggerFactory loggerFactory, final String modId, @Token final String token, final CompatibilityLayer compatibilityLayer) {
        super(factory, loggerFactory, SimpleConfig.read(
                compatibilityLayer.configDirectory().resolve("faststats").resolve("config.properties"), loggerFactory,
                compatibilityLayer.getEnvironment() == CompatibilityLayer.Environment.CLIENT), "forge", token);
        this.modVersion = compatibilityLayer.modVersion(modId).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        this.modId = modId;
        this.compatibilityLayer = compatibilityLayer;
        this.consentRequired = factory.consentRequired;
        initializeManagedServices(factory);
        FastStatsRegistry.instance().register(this, factory.consentRequired);
        switch (compatibilityLayer.getEnvironment()) {
            case CLIENT -> ready();
            case SERVER -> compatibilityLayer.onServerLifecycle(this::ready, this::shutdown);
        }
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return switch (compatibilityLayer.getEnvironment()) {
                    case CLIENT -> new ForgeMetricsClient(this, modVersion, compatibilityLayer);
                    case SERVER -> new ForgeMetricsServer(this, modVersion, compatibilityLayer);
                };
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        final var config = (SimpleConfig) getConfig();
        if (!config.preSubmissionStart(this)) return false;
        if (!consentRequired || config.consentScreenShown()) return true;
        return compatibilityLayer.getEnvironment() != CompatibilityLayer.Environment.CLIENT;
    }

    @Override
    public String getProjectName() {
        return modId;
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        tasks.add(executor.scheduleAtFixedRate(task, initialDelay, period, unit));
    }

    @Override
    public void shutdown() {
        FastStatsRegistry.instance().unregister(this);
        super.shutdown();
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
        executor.shutdown();
    }

    public static final class Factory extends SimpleContext.Factory<ForgeContext, Factory> {
        private final String modId;
        private final @Token String token;
        private boolean consentRequired = false;

        public Factory(final String modId, @Token final String token) {
            this.modId = modId;
            this.token = token;
        }

        /**
         * Requires the user to explicitly give consent on the client.
         * <p>
         * When at least one mod requires consent, the consent screen is opened
         * on the client until it was shown once, nothing is submitted before that.
         * By default, no consent screen is shown.
         *
         * @return this factory
         * @since 0.31.0
         */
        @Contract(value = " -> this", mutates = "this")
        public Factory requireConsent() {
            this.consentRequired = true;
            return this;
        }

        @Override
        public ForgeContext create() {
            final var logger = org.slf4j.LoggerFactory.getLogger(modId);
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                switch (level) {
                    case INFO -> logger.info(message, throwable);
                    case ERROR -> logger.error(message, throwable);
                    case WARN -> logger.warn(message, throwable);
                }
            });
            return new ForgeContext(this, loggerFactory, modId, token);
        }
    }
}
