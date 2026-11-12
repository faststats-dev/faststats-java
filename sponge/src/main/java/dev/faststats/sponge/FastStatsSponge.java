package dev.faststats.sponge;

import com.google.inject.Inject;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;

/**
 * FastStats for Sponge plugins.
 * <p>
 * Inject this class into your plugin, build the context,
 * make it ready once the server started and shut it down when it stops:
 * <pre>{@code
 * private final FastStatsContext context;
 *
 * @Inject
 * public ExamplePlugin(final FastStatsSponge fastStats) {
 *     this.context = fastStats.builder()
 *             .metrics(Metrics.Factory::create)
 *             .errorTrackerService(ErrorTracker.contextAware())
 *             .build();
 * }
 *
 * @Listener
 * public void onServerStarted(final StartedEngineEvent<Server> event) {
 *     context.ready();
 * }
 *
 * @Listener
 * public void onServerStopping(final StoppingEngineEvent<Server> event) {
 *     context.shutdown();
 * }
 * }</pre>
 * <p>
 * Without dependency injection, use {@link #builder(PluginContainer, Path)} instead.
 * <p>
 * Submission additionally respects the metrics collection state of the Sponge metrics config.
 *
 * @since 0.33.0
 */
public final class FastStatsSponge {
    private final PluginContainer plugin;
    private final Path configDirectory;

    /**
     * Creates a new instance for the given plugin.
     *
     * @param plugin          the plugin container
     * @param configDirectory the shared Sponge config directory
     * @apiNote This instance is meant to be injected into your plugin.
     * @since 0.33.0
     */
    @Inject
    public FastStatsSponge(
            final PluginContainer plugin,
            @ConfigDir(sharedRoot = true) final Path configDirectory
    ) {
        this.plugin = plugin;
        this.configDirectory = configDirectory;
    }

    /**
     * Creates a new context builder for the injected plugin.
     *
     * @return a new context builder
     * @see #builder(PluginContainer, Path)
     * @since 0.33.0
     */
    @Contract(value = " -> new", mutates = "io")
    public FastStatsContext.Builder builder() {
        return builder(plugin, configDirectory);
    }

    /**
     * Creates a new context builder for the given plugin.
     * <p>
     * The shared FastStats config is read from {@code faststats/config.properties}
     * in the shared config directory right away.
     * If no {@linkplain FastStatsContext.Builder#token(String) token} is set, the token injected at build time
     * is read from {@code META-INF/faststats/<plugin id>.token}.
     * If no token was injected, a warning is logged and the built context is disabled.
     *
     * @param plugin          the plugin container
     * @param configDirectory the shared Sponge config directory, injected with {@code @ConfigDir(sharedRoot = true)}
     * @return a new context builder
     * @since 0.33.0
     */
    @Contract(value = "_, _ -> new", mutates = "io")
    public static FastStatsContext.Builder builder(
            final PluginContainer plugin,
            @ConfigDir(sharedRoot = true) final Path configDirectory
    ) {
        final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
            switch (level) {
                case INFO -> {
                    if (throwable == null) plugin.logger().info(message);
                    else plugin.logger().info(message, throwable);
                }
                case ERROR -> {
                    if (throwable == null) plugin.logger().error(message);
                    else plugin.logger().error(message, throwable);
                }
                case WARN -> {
                    if (throwable == null) plugin.logger().warn(message);
                    else plugin.logger().warn(message, throwable);
                }
            }
        });
        final var config = SpongeConfig.read(plugin, configDirectory
                .resolve("faststats").resolve("config.properties"), loggerFactory);
        final var id = plugin.metadata().id();
        return new SimpleContext.Builder("sponge")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(id)
                .tokenResource(classLoader(plugin), id)
                .scheduler((task, initialDelay, period, unit) -> Sponge.asyncScheduler().submit(Task.builder()
                        .delay(initialDelay, unit)
                        .interval(period, unit)
                        .plugin(plugin)
                        .execute(task)
                        .build())::cancel)
                .defaultMetrics((context, metrics) -> {
                    final var implementation = Sponge.platform().container(Platform.Component.IMPLEMENTATION);
                    metrics.addProperty("game_version", Sponge.platform().minecraftVersion().name());
                    metrics.addProperty("online_mode", Sponge.server().isOnlineModeEnabled());
                    metrics.addProperty("platform_version", implementation.metadata().version().toString()); // todo: double check
                    metrics.addProperty("player_count", Sponge.server().onlinePlayers().size());
                    metrics.addProperty("plugin_version", plugin.metadata().version().toString());
                    metrics.addProperty("server_type", implementation.metadata().id()); // todo: double check
                })
                .onReady(context -> {
                    if (config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                });
    }

    private static ClassLoader classLoader(final PluginContainer plugin) {
        try {
            // the instance is not available yet while the plugin is being constructed
            final var instance = plugin.instance();
            if (instance != null) return instance.getClass().getClassLoader();
        } catch (final RuntimeException ignored) {
        }
        return FastStatsSponge.class.getClassLoader();
    }
}
