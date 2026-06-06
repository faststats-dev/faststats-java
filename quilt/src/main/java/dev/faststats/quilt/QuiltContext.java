package dev.faststats.quilt;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Contract;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Quilt FastStats context.
 *
 * @since 0.25.2
 */
public final class QuiltContext extends SimpleContext {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();
    private final ModContainer mod;

    private QuiltContext(final Factory factory, final String modId, @Token final String token) {
        super(factory, SimpleConfig.read(QuiltLoader.getConfigDir().resolve("faststats").resolve("config.properties")), "quilt", token);
        this.mod = QuiltLoader.getModContainer(modId).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        // todo: proper client/server support
        return new QuiltMetrics.Factory(this, mod);
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
    }

    @Override
    public String getProjectName() {
        return mod.metadata().id();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        tasks.add(executor.scheduleAtFixedRate(task, initialDelay, period, unit));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
        executor.shutdown();
    }

    public static final class Factory extends SimpleContext.Factory<QuiltContext, Factory> {
        private final String modId;
        private final @Token String token;

        public Factory(final String modId, @Token final String token) {
            this.modId = modId;
            this.token = token;
        }

        @Override
        public QuiltContext create() {
            return new QuiltContext(this, modId, token);
        }
    }
}
