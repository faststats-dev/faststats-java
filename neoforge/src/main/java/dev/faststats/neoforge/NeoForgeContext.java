package dev.faststats.neoforge;

import dev.faststats.Metrics;
import dev.faststats.FastStatsRegistry;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import dev.faststats.neoforge.compat.CompatibilityLayer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforgespi.language.IModInfo;
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
 * NeoForge FastStats context.
 *
 * @since 0.27.0
 */
public final class NeoForgeContext extends SimpleContext {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();
    private final CompatibilityLayer compatibilityLayer;
    private final IModInfo mod;

    private NeoForgeContext(final Factory factory, final LoggerFactory loggerFactory, final String modId, @Token final String token) {
        super(factory, loggerFactory, SimpleConfig.read(configPath(), loggerFactory), "neoforge", token);
        this.mod = ModList.get().getModContainerById(modId).map(ModContainer::getModInfo).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        this.compatibilityLayer = ServiceLoader.load(CompatibilityLayer.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No NeoForge compatibility layer found"));
        initializeManagedServices(factory);
        FastStatsRegistry.instance().register(this);
        switch (compatibilityLayer.getEnvironment()) {
            case CLIENT -> ready();
            case SERVER -> {
                NeoForge.EVENT_BUS.addListener((final ServerStartedEvent event) -> ready());
                NeoForge.EVENT_BUS.addListener((final ServerStoppingEvent event) -> shutdown());
            }
        }
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return switch (compatibilityLayer.getEnvironment()) {
                    case CLIENT -> new NeoForgeMetricsClient(this, mod, compatibilityLayer);
                    case SERVER -> new NeoForgeMetricsServer(this, mod, compatibilityLayer);
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
        return mod.getModId();
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

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("faststats").resolve("config.properties");
    }

    public static final class Factory extends SimpleContext.Factory<NeoForgeContext, Factory> {
        private final String modId;
        private final @Token String token;

        public Factory(final String modId, @Token final String token) {
            this.modId = modId;
            this.token = token;
        }

        @Override
        public NeoForgeContext create() {
            final var logger = org.slf4j.LoggerFactory.getLogger(modId);
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                switch (level) {
                    case INFO -> logger.info(message, throwable);
                    case ERROR -> logger.error(message, throwable);
                    case WARN -> logger.warn(message, throwable);
                }
            });
            return new NeoForgeContext(this, loggerFactory, modId, token);
        }
    }
}
