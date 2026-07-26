package dev.faststats.hytale;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Hytale FastStats context.
 *
 * @since 0.24.0
 */
public final class HytaleContext extends SimpleContext {
    private final Set<TaskRegistration> registrations = new CopyOnWriteArraySet<>();
    private final JavaPlugin plugin;

    private HytaleContext(final Factory factory, final LoggerFactory loggerFactory, final JavaPlugin plugin, @Token final String token) {
        super(factory, loggerFactory, SimpleConfig.read(plugin.getDataDirectory().toAbsolutePath().getParent()
                .resolve("faststats").resolve("config.properties"), loggerFactory
        ), "hytale", token);
        this.plugin = plugin;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                // todo: add client support?
                return new HytaleMetricsImpl(this, plugin);
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return plugin.getName();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        @SuppressWarnings("unchecked") final var scheduledTask = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(task, initialDelay, period, unit);
        registrations.add(plugin.getTaskRegistry().registerTask(scheduledTask));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        registrations.forEach(TaskRegistration::unregister);
        registrations.clear();
    }

    public static final class Factory extends SimpleContext.Factory<HytaleContext, Factory> {
        private final JavaPlugin plugin;
        private final @Token String token;

        public Factory(final JavaPlugin plugin, @Token final String token) {
            this.plugin = plugin;
            this.token = token;
        }

        @Override
        public HytaleContext create() {
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                final var api = switch (level) {
                    case INFO -> plugin.getLogger().atInfo();
                    case ERROR -> plugin.getLogger().atSevere();
                    case WARN -> plugin.getLogger().atWarning();
                };
                if (throwable == null) api.log(message);
                else api.log(message, throwable);
            });
            return new HytaleContext(this, loggerFactory, plugin, token);
        }
    }
}
