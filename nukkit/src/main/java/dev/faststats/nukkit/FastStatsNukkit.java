package dev.faststats.nukkit;

import cn.nukkit.plugin.PluginBase;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * FastStats for Nukkit plugins.
 * <p>
 * Create the context in your plugin, make it ready in {@code onEnable} and shut it down in {@code onDisable}:
 * <pre>{@code
 * private final FastStatsContext context = FastStatsNukkit.builder(this)
 *         .metrics(Metrics.Factory::create)
 *         .errorTrackerService(ErrorTracker.contextAware())
 *         .build();
 *
 * @Override
 * public void onEnable() {
 *     context.ready();
 * }
 *
 * @Override
 * public void onDisable() {
 *     context.shutdown();
 * }
 * }</pre>
 *
 * @since 0.33.0
 */
public final class FastStatsNukkit {
    private FastStatsNukkit() {
    }

    /**
     * Creates a new context builder for the given plugin.
     * <p>
     * The shared FastStats config is read from {@code plugins/faststats/config.properties} right away.
     * If no {@linkplain FastStatsContext.Builder#token(String) token} is set, the token injected at build time
     * is read from {@code META-INF/faststats/<plugin name>.token}.
     * If no token was injected, a warning is logged and the built context is disabled.
     * <p>
     * The built context must be made {@linkplain FastStatsContext#ready() ready} in or after {@code onEnable},
     * as Nukkit rejects tasks of plugins that are not enabled yet.
     *
     * @param plugin the plugin
     * @return a new context builder
     * @since 0.33.0
     */
    @Contract(value = "_ -> new", mutates = "io")
    public static FastStatsContext.Builder builder(final PluginBase plugin) {
        final var loggerFactory = new PlatformLoggerFactory((level, t, message) -> {
            switch (level) {
                case INFO -> plugin.getLogger().info(message, t);
                case ERROR -> plugin.getLogger().error(message, t);
                case WARN -> plugin.getLogger().warning(message, t);
            }
        });
        final var server = plugin.getServer();
        final var config = SimpleConfig.read(Path.of(server.getPluginPath())
                .resolve("faststats").resolve("config.properties"), loggerFactory);
        return new SimpleContext.Builder("nukkit")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(plugin.getName())
                .tokenResource(plugin.getClass().getClassLoader(), plugin.getName())
                .scheduler((task, initialDelay, period, unit) -> server.getScheduler().scheduleDelayedRepeatingTask(
                        plugin, task, toTicks(initialDelay, unit), toTicks(period, unit), true
                )::cancel)
                .defaultMetrics((context, metrics) -> {
                    metrics.addProperty("game_version", server.getVersion());
                    metrics.addProperty("online_mode", server.xboxAuth);
                    metrics.addProperty("platform_version", server.getNukkitVersion());
                    metrics.addProperty("player_count", server.getOnlinePlayersCount());
                    metrics.addProperty("plugin_version", plugin.getDescription().getVersion());
                    metrics.addProperty("server_type", server.getName());
                })
                .onReady(context -> {
                    // Nukkit rejects tasks of plugins that are not enabled yet
                    if (!plugin.isEnabled()) throw new IllegalStateException(
                            "The FastStats context of " + plugin.getName() + " must be made ready in or after onEnable"
                    );
                    if (config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                });
    }

    private static int toTicks(final long time, final TimeUnit unit) {
        return Math.max(1, (int) (unit.toMillis(time) / 50L));
    }
}
