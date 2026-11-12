package dev.faststats.bungee;

import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.config.CustomConfig;
import dev.faststats.internal.PlatformLoggerFactory;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Contract;

/**
 * FastStats for BungeeCord plugins.
 * <p>
 * Create the context in your plugin, make it ready in {@code onEnable} and shut it down in {@code onDisable}:
 * <pre>{@code
 * private final FastStatsContext context = FastStatsBungee.builder(this)
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
public final class FastStatsBungee {
    private FastStatsBungee() {
    }

    /**
     * Creates a new context builder for the given plugin.
     * <p>
     * The shared FastStats config is read from {@code plugins/faststats/config.properties} right away.
     * If no {@linkplain FastStatsContext.Builder#token(String) token} is set, the token injected at build time
     * is read from {@code META-INF/faststats/<plugin name>.token}.
     * If no token was injected, a warning is logged and the built context is disabled.
     *
     * @param plugin the plugin
     * @return a new context builder
     * @since 0.33.0
     */
    @Contract(value = "_ -> new", mutates = "io")
    public static FastStatsContext.Builder builder(final Plugin plugin) {
        final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
            plugin.getLogger().log(level.getLevel(), message, throwable);
        });
        final var server = plugin.getProxy();
        final var config = CustomConfig.builder(server.getPluginsFolder().toPath()
                .resolve("faststats").resolve("config.properties"), loggerFactory).build();
        final var name = plugin.getDescription().getName();
        return new SimpleContext.Builder("bungeecord")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(name)
                .tokenResource(plugin.getClass().getClassLoader(), name)
                .scheduler((task, initialDelay, period, unit) -> server.getScheduler()
                        .schedule(plugin, task, initialDelay, period, unit)::cancel)
                .defaultMetrics((context, metrics) -> {
                    metrics.addProperty("online_mode", server.getConfig().isOnlineMode());
                    metrics.addProperty("platform_version", server.getVersion());
                    metrics.addProperty("player_count", server.getOnlineCount());
                    metrics.addProperty("plugin_version", plugin.getDescription().getVersion());
                    metrics.addProperty("server_type", server.getName());
                })
                .onReady(context -> {
                    if (config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                });
    }
}
