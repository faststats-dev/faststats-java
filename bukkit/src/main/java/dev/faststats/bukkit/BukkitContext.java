package dev.faststats.bukkit;

import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.CustomConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Bukkit FastStats context.
 *
 * @since 0.24.0
 */
public final class BukkitContext extends SimpleContext {
    private final Set<Runnable> cancellations = new CopyOnWriteArraySet<>();
    private final Plugin plugin;

    private BukkitContext(final Factory factory, final LoggerFactory loggerFactory, final Plugin plugin, @Token final String token) {
        super(factory, loggerFactory, CustomConfig.builder(getConfigPath(plugin), loggerFactory).build(), "bukkit", token);
        this.plugin = plugin;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected BukkitMetrics.Factory metricsFactory() {
        return new BukkitMetricsImpl.Factory(this, plugin);
    }

    @Override
    public void ready() {
        try {
            if (ready) return;
            Class.forName("com.destroystokyo.paper.event.server.ServerExceptionEvent");
            plugin.getServer().getPluginManager().registerEvents(new PaperEventListener(plugin, this), plugin);
        } catch (final ClassNotFoundException ignored) {
        } finally {
            super.ready();
        }
    }

    private static Path getConfigPath(final Plugin plugin) {
        return getPluginsFolder(plugin).resolve("faststats").resolve("config.properties");
    }

    private static Path getPluginsFolder(final Plugin plugin) {
        try {
            return plugin.getServer().getPluginsFolder().toPath();
        } catch (final NoSuchMethodError e) {
            return plugin.getDataFolder().getParentFile().toPath();
        }
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((CustomConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return plugin.getName();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        try {
            final var scheduledTask = plugin.getServer().getAsyncScheduler().runAtFixedRate(
                    plugin, ignored -> task.run(), initialDelay, period, unit
            );
            cancellations.add(scheduledTask::cancel);
        } catch (final Throwable t) {
            final var scheduledTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                    plugin, task, toTicks(initialDelay, unit), toTicks(period, unit)
            );
            cancellations.add(scheduledTask::cancel);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        cancellations.forEach(Runnable::run);
        cancellations.clear();
    }

    public static final class Factory extends SimpleContext.Factory<BukkitContext, Factory> {
        private final Plugin plugin;
        private final @Token String token;

        public Factory(final Plugin plugin, @Token final String token) {
            this.plugin = plugin;
            this.token = token;
        }

        @Override
        public BukkitContext create() {
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                plugin.getLogger().log(level.getLevel(), message, throwable);
            });
            return new BukkitContext(this, loggerFactory, plugin, token);
        }
    }

    private static long toTicks(final long time, final TimeUnit unit) {
        return Math.max(1L, unit.toMillis(time) / 50L);
    }
}
