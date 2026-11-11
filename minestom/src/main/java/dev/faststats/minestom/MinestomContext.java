package dev.faststats.minestom;

import dev.faststats.ErrorTracker;
import dev.faststats.ErrorTrackerService;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.PlatformLoggerFactory;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Minestom FastStats context.
 *
 * @since 0.24.0
 */
public final class MinestomContext extends SimpleContext {
    private final Set<Task> tasks = new CopyOnWriteArraySet<>();

    MinestomContext(final Factory factory, final dev.faststats.internal.LoggerFactory loggerFactory, @Token final @Nullable String token) {
        super(factory, loggerFactory, SimpleConfig.read(Path.of("faststats", "config.properties"), loggerFactory), "minestom", token);
        initializeServices(factory);
    }

    @Override
    public void ready() {
        if (!ready) errorTrackerService().map(ErrorTrackerService::globalErrorTracker).ifPresent(errorTracker -> {
            final var handler = MinecraftServer.getExceptionManager().getExceptionHandler();
            MinecraftServer.getExceptionManager().setExceptionHandler(error -> {
                handler.handleException(error);
                if (!ErrorTracker.isSameLoader(getClass().getClassLoader(), error)) return;
                errorTracker.trackError(error);
            });
        });
        super.ready();
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected MinestomMetrics.Factory metricsFactory() {
        return new MinestomMetricsImpl.Factory(this);
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return MinecraftServer.getBrandName();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        final var scheduleTask = MinecraftServer.getSchedulerManager().scheduleTask(
                task,
                TaskSchedule.duration(initialDelay, unit.toChronoUnit()),
                TaskSchedule.duration(period, unit.toChronoUnit())
        );
        tasks.add(scheduleTask);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(Task::cancel);
        tasks.clear();
    }

    public static final class Factory extends SimpleContext.Factory<MinestomContext, Factory> {
        @Nullable
        private final @Token String token;

        /**
         * Creates a new factory that reads the token injected at build time
         * from {@code META-INF/faststats/minestom.token}.
         * If no token was injected, a warning is logged and the created context is disabled.
         *
         * @see SimpleContext.Factory#readToken(ClassLoader, String)
         * @since 0.32.0
         */
        public Factory() {
            this.token = null;
        }

        public Factory(@Token final String token) {
            this.token = token;
        }

        @Override
        @SuppressWarnings("PatternValidation")
        public MinestomContext create() {
            final var logger = LoggerFactory.getLogger("FastStats");
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
            final var token = resolveToken(this.token, Factory.class.getClassLoader(), "minestom");
            return new MinestomContext(this, loggerFactory, token);
        }
    }
}
