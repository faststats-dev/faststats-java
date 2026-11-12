package dev.faststats.hytale;

import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.config.CustomConfig;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;

import java.util.concurrent.ScheduledFuture;

/**
 * FastStats for Hytale server plugins.
 * <p>
 * Create the context in your plugin, make it ready once the plugin started and shut it down when it stops:
 * <pre>{@code
 * private final FastStatsContext context = FastStatsHytale.builder(this)
 *         .metrics(Metrics.Factory::create)
 *         .errorTrackerService(ErrorTracker.contextAware())
 *         .build();
 *
 * @Override
 * protected void start() {
 *     context.ready();
 * }
 *
 * @Override
 * protected void shutdown() {
 *     context.shutdown();
 * }
 * }</pre>
 *
 * @since 0.33.0
 */
public final class FastStatsHytale {
    private FastStatsHytale() {
    }

    /**
     * Creates a new context builder for the given plugin.
     * <p>
     * The shared FastStats config is read from {@code faststats/config.properties}
     * next to the plugin data directories right away.
     * If no {@linkplain FastStatsContext.Builder#token(String) token} is set, the token injected at build time
     * is read from {@code META-INF/faststats/<plugin name>.token}.
     * If no token was injected, a warning is logged and the built context is disabled.
     *
     * @param plugin the plugin
     * @return a new context builder
     * @since 0.33.0
     */
    @Contract(value = "_ -> new", mutates = "io")
    public static FastStatsContext.Builder builder(final JavaPlugin plugin) {
        final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
            final var api = switch (level) {
                case INFO -> plugin.getLogger().atInfo();
                case ERROR -> plugin.getLogger().atSevere();
                case WARN -> plugin.getLogger().atWarning();
            };
            if (throwable == null) api.log(message);
            else api.log(message, throwable);
        });
        final var config = CustomConfig.builder(plugin.getDataDirectory().toAbsolutePath().getParent()
                .resolve("faststats").resolve("config.properties"), loggerFactory).build();
        final var pluginVersion = plugin.getManifest().getVersion().toString();
        return new SimpleContext.Builder("hytale")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(plugin.getName())
                .tokenResource(plugin.getClass().getClassLoader(), plugin.getName())
                .scheduler((task, initialDelay, period, unit) -> {
                    @SuppressWarnings("unchecked") final var scheduledTask = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR
                            .scheduleAtFixedRate(task, initialDelay, period, unit);
                    return plugin.getTaskRegistry().registerTask(scheduledTask)::unregister;
                })
                .defaultMetrics((context, metrics) -> {
                    metrics.addProperty("game_version", ManifestUtil.getImplementationVersion());
                    metrics.addProperty("online_mode", ServerAuthManager.getInstance().getAuthMode() != ServerAuthManager.AuthMode.NONE);
                    metrics.addProperty("platform_version", ManifestUtil.getVersion());
                    metrics.addProperty("player_count", Universe.get().getPlayerCount());
                    metrics.addProperty("server_type", "Hytale Server");
                    metrics.addProperty("plugin_version", pluginVersion);
                })
                .onReady(context -> {
                    if (config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                });
    }
}
