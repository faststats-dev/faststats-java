package dev.faststats.fabric;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import dev.faststats.fabric.compat.CompatibilityLayer;
import dev.faststats.internal.PlatformLoggerFactory;
import dev.faststats.screen.registry.FastStatsRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fabric FastStats context.
 *
 * @since 0.24.0
 */
public final class FabricContext extends SimpleContext {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();
    private final CompatibilityLayer compatibilityLayer;
    private final ModContainer mod;

    private FabricContext(final Factory factory, final dev.faststats.internal.LoggerFactory loggerFactory, final String modId, @Token final String token) {
        super(factory, loggerFactory, SimpleConfig.read(configPath(), loggerFactory), "fabric", token);
        this.mod = FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        this.compatibilityLayer = ServiceLoader.load(CompatibilityLayer.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Fabric compatibility layer found"));
        initializeManagedServices(factory);
        FastStatsRegistry.instance().register(this);
        switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT -> {
                ready();
                ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
            }
            case SERVER -> {
                ServerLifecycleEvents.SERVER_STARTED.register(server -> ready());
                ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutdown());
            }
        }
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return switch (FabricLoader.getInstance().getEnvironmentType()) {
                    case CLIENT -> new FabricMetricsClient(this, mod, compatibilityLayer);
                    case SERVER -> new FabricMetricsServer(this, mod, compatibilityLayer);
                };
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return mod.getMetadata().getId();
    }

    @Override
    public void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
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

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("faststats").resolve("config.properties");
    }

    public static final class Factory extends SimpleContext.Factory<FabricContext, Factory> {
        private final String modId;
        private final @Token String token;

        public Factory(final String modId, @Token final String token) {
            this.modId = modId;
            this.token = token;
        }

        @Override
        public FabricContext create() {
            final var logger = org.slf4j.LoggerFactory.getLogger(modId);
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                switch (level) {
                    case INFO -> {
                        if (throwable == null) logger.info(message);
                        else logger.info(message, throwable);
                    }
                    case ERROR -> {
                        if (throwable == null) logger.error(message);
                        else logger.error(message, throwable);
                    }
                    case WARN -> {
                        if (throwable == null) logger.warn(message);
                        else logger.warn(message, throwable);
                    }
                }
            });
            return new FabricContext(this, loggerFactory, modId, token);
        }
    }
}
