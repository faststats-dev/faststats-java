package dev.faststats.minestom;

import dev.faststats.ErrorTracker;
import dev.faststats.ErrorTrackerService;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.PlatformLoggerFactory;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Contract;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * FastStats for Minestom servers.
 * <p>
 * Build the context during startup, make it ready once the server started and shut it down when it stops:
 * <pre>{@code
 * final var server = MinecraftServer.init();
 * final var context = FastStatsMinestom.builder()
 *         .metrics(Metrics.Factory::create)
 *         .errorTrackerService(ErrorTracker.contextAware())
 *         .build();
 * MinecraftServer.getSchedulerManager().buildShutdownTask(context::shutdown);
 * server.start("0.0.0.0", 25565);
 * context.ready();
 * }</pre>
 *
 * @since 0.33.0
 */
public final class FastStatsMinestom {
    private FastStatsMinestom() {
    }

    /**
     * Creates a new context builder for the server.
     * <p>
     * The FastStats config is read from {@code faststats/config.properties}
     * in the working directory right away.
     * If no {@linkplain FastStatsContext.Builder#token(String) token} is set, the token injected at build time
     * is read from {@code META-INF/faststats/minestom.token}.
     * If no token was injected, a warning is logged and the built context is disabled.
     * <p>
     * The server must be {@linkplain MinecraftServer#init() initialized} before calling this method.
     *
     * @return a new context builder
     * @since 0.33.0
     */
    @Contract(value = " -> new", mutates = "io")
    public static FastStatsContext.Builder builder() {
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
        final var config = SimpleConfig.read(Path.of("faststats", "config.properties"), loggerFactory);
        return new SimpleContext.Builder("minestom")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(MinecraftServer.getBrandName())
                .tokenResource(FastStatsMinestom.class.getClassLoader(), "minestom")
                .scheduler((task, initialDelay, period, unit) -> MinecraftServer.getSchedulerManager().scheduleTask(
                        task,
                        TaskSchedule.duration(initialDelay, unit.toChronoUnit()),
                        TaskSchedule.duration(period, unit.toChronoUnit())
                )::cancel)
                .defaultMetrics((context, metrics) -> {
                    metrics.addProperty("game_version", MinecraftServer.VERSION_NAME);
                    metrics.addProperty("online_mode", !(MinecraftServer.process().auth() instanceof Auth.Offline));
                    metrics.addProperty("player_count", MinecraftServer.getConnectionManager().getOnlinePlayerCount());
                    metrics.addProperty("server_type", "Minestom");
                    metrics.addProperty("platform_version", MinecraftServer.getBrandName() + " " + MinecraftServer.VERSION_NAME);
                })
                .onReady(context -> {
                    if (config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                    registerExceptionHandler(context);
                });
    }

    private static void registerExceptionHandler(final FastStatsContext context) {
        context.errorTrackerService().map(ErrorTrackerService::globalErrorTracker).ifPresent(errorTracker -> {
            final var handler = MinecraftServer.getExceptionManager().getExceptionHandler();
            MinecraftServer.getExceptionManager().setExceptionHandler(error -> {
                handler.handleException(error);
                if (!ErrorTracker.isSameLoader(FastStatsMinestom.class.getClassLoader(), error)) return;
                errorTracker.trackError(error);
            });
        });
    }
}
