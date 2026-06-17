package dev.faststats.fabric;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Contract;

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
    private final ModContainer mod;

    private FabricContext(final Factory factory, final String modId, @Token final String token) {
        super(factory, SimpleConfig.read(FabricLoader.getInstance().getConfigDir().resolve("faststats").resolve("config.properties")), "fabric", token);
        this.mod = FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        initializeServices(factory);
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
                    case CLIENT -> new FabricMetricsClient(this, mod);
                    case SERVER -> new FabricMetricsServer(this, mod);
                };
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart(getProjectName());
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
        super.shutdown();
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
        executor.shutdown();
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
            return new FabricContext(this, modId, token);
        }
    }
}
