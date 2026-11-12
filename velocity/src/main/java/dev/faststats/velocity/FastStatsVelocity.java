package dev.faststats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.config.CustomConfig;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;

/**
 * FastStats for Velocity plugins.
 * <p>
 * Inject this class into your plugin, build the context,
 * make it ready once the proxy initialized and shut it down when it stops:
 * <pre>{@code
 * private final FastStatsContext context;
 *
 * @Inject
 * public ExamplePlugin(final FastStatsVelocity fastStats) {
 *     this.context = fastStats.builder()
 *             .metrics(Metrics.Factory::create)
 *             .errorTrackerService(ErrorTracker.contextAware())
 *             .build();
 * }
 *
 * @Subscribe
 * public void onProxyInitialize(final ProxyInitializeEvent event) {
 *     context.ready();
 * }
 *
 * @Subscribe
 * public void onProxyShutdown(final ProxyShutdownEvent event) {
 *     context.shutdown();
 * }
 * }</pre>
 * <p>
 * Without dependency injection, use {@link #builder(PluginContainer, ProxyServer, Logger, Path)} instead.
 *
 * @since 0.33.0
 */
public final class FastStatsVelocity {
    private final PluginContainer plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    /**
     * Creates a new instance for the given plugin.
     *
     * @param plugin        the plugin container
     * @param server        the proxy server
     * @param logger        the plugin logger
     * @param dataDirectory the plugin data directory
     * @apiNote This instance is meant to be injected into your plugin.
     * @since 0.33.0
     */
    @Inject
    public FastStatsVelocity(
            final PluginContainer plugin,
            final ProxyServer server,
            final Logger logger,
            @DataDirectory final Path dataDirectory
    ) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /**
     * Creates a new context builder for the injected plugin.
     *
     * @return a new context builder
     * @see #builder(PluginContainer, ProxyServer, Logger, Path)
     * @since 0.33.0
     */
    @Contract(value = " -> new", mutates = "io")
    public FastStatsContext.Builder builder() {
        return builder(plugin, server, logger, dataDirectory);
    }

    /**
     * Creates a new context builder for the given plugin.
     * <p>
     * The shared FastStats config is read from {@code plugins/faststats/config.properties} right away.
     * If no {@linkplain FastStatsContext.Builder#token(String) token} is set, the token injected at build time
     * is read from {@code META-INF/faststats/<plugin id>.token}.
     * If no token was injected, a warning is logged and the built context is disabled.
     *
     * @param plugin        the plugin container
     * @param server        the proxy server
     * @param logger        the plugin logger
     * @param dataDirectory the plugin data directory, injected with {@code @DataDirectory}
     * @return a new context builder
     * @since 0.33.0
     */
    @Contract(value = "_, _, _, _ -> new", mutates = "io")
    public static FastStatsContext.Builder builder(
            final PluginContainer plugin,
            final ProxyServer server,
            final Logger logger,
            @DataDirectory final Path dataDirectory
    ) {
        final var loggerFactory = new PlatformLoggerFactory((level, t, message) -> {
            switch (level) {
                case INFO -> logger.info(message, t);
                case ERROR -> logger.error(message, t);
                case WARN -> logger.warn(message, t);
            }
        });
        final var config = CustomConfig.builder(dataDirectory.resolveSibling("faststats")
                .resolve("config.properties"), loggerFactory).build();
        final var id = plugin.getDescription().getId();
        return new SimpleContext.Builder("velocity")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(id)
                .tokenResource(plugin.getInstance()
                        .map(instance -> instance.getClass().getClassLoader())
                        .orElse(FastStatsVelocity.class.getClassLoader()), id)
                .scheduler((task, initialDelay, period, unit) -> server.getScheduler()
                        .buildTask(plugin, task)
                        .delay(Duration.ofMillis(unit.toMillis(initialDelay)))
                        .repeat(Duration.ofMillis(unit.toMillis(period)))
                        .schedule()::cancel)
                .defaultMetrics((context, metrics) -> {
                    metrics.addProperty("online_mode", server.getConfiguration().isOnlineMode());
                    metrics.addProperty("platform_version", server.getVersion().getVersion());
                    metrics.addProperty("player_count", server.getPlayerCount());
                    plugin.getDescription().getVersion().ifPresent(s -> metrics.addProperty("plugin_version", s));
                    metrics.addProperty("server_type", server.getVersion().getName());
                })
                .onReady(context -> {
                    if (config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                });
    }
}
