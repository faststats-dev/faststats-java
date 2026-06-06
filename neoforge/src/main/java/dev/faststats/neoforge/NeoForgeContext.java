package dev.faststats.neoforge;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Contract;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * NeoForge FastStats context.
 *
 * @since 0.25.2
 */
public final class NeoForgeContext extends SimpleContext {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();
    private final IModInfo mod;

    private NeoForgeContext(final Factory factory, final String modId, @Token final String token) {
        super(factory, SimpleConfig.read(FMLPaths.CONFIGDIR.get().resolve("faststats").resolve("config.properties")), "neoforge", token);
        this.mod = ModList.get().getModFileById(modId).getMods().stream().filter(mod -> mod.getModId().equals(modId)).findFirst().orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        // todo: proper client/server support
        return new NeoForgeMetrics.Factory(this, mod);
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
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
        super.shutdown();
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
        executor.shutdown();
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
            return new NeoForgeContext(this, modId, token);
        }
    }
}
