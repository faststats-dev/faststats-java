package dev.faststats.bungee;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * BungeeCord FastStats context.
 *
 * @since 0.24.0
 */
public final class BungeeContext extends SimpleContext {
    private final Set<ScheduledTask> tasks = new CopyOnWriteArraySet<>();
    private final Plugin plugin;

    private BungeeContext(final Factory factory, final LoggerFactory loggerFactory, final Plugin plugin, @Token final String token) {
        super(factory, loggerFactory, SimpleConfig.read(plugin.getProxy().getPluginsFolder().toPath()
                .resolve("faststats").resolve("config.properties"), loggerFactory
        ), "bungeecord", token);
        this.plugin = plugin;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new BungeeMetricsImpl(this, plugin);
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return plugin.getDescription().getName();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        tasks.add(plugin.getProxy().getScheduler().schedule(plugin, task, initialDelay, period, unit));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    public static final class Factory extends SimpleContext.Factory<BungeeContext, Factory> {
        private final Plugin plugin;
        @Nullable
        private final @Token String token;

        /**
         * Creates a new factory that reads the token injected at build time
         * from {@code META-INF/faststats/<plugin name>.token}.
         *
         * @param plugin the plugin
         * @see SimpleContext.Factory#readToken(ClassLoader, String)
         * @since 0.32.0
         */
        public Factory(final Plugin plugin) {
            this.plugin = plugin;
            this.token = null;
        }

        public Factory(final Plugin plugin, @Token final String token) {
            this.plugin = plugin;
            this.token = token;
        }

        @Override
        @SuppressWarnings("PatternValidation")
        public BungeeContext create() {
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                plugin.getLogger().log(level.getLevel(), message, throwable);
            });
            final var token = this.token != null ? this.token : readToken(plugin.getClass().getClassLoader(), plugin.getDescription().getName());
            return new BungeeContext(this, loggerFactory, plugin, token);
        }
    }
}
