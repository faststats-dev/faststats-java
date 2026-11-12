package dev.faststats.bukkit;

import com.google.gson.JsonObject;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.config.CustomConfig;
import dev.faststats.internal.PlatformLoggerFactory;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * FastStats for Bukkit, Spigot, Paper and Folia plugins.
 * <p>
 * Create the context in your plugin, make it ready in {@code onEnable} and shut it down in {@code onDisable}:
 * <pre>{@code
 * private final FastStatsContext context = FastStatsBukkit.builder(this)
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
public final class FastStatsBukkit {
    private FastStatsBukkit() {
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
     * as Bukkit rejects tasks and listeners of plugins that are not enabled yet.
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
        final var config = CustomConfig.builder(getConfigPath(plugin), loggerFactory).build();
        return new SimpleContext.Builder("bukkit")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(plugin.getName())
                .tokenResource(plugin.getClass().getClassLoader(), plugin.getName())
                .scheduler(scheduler(plugin))
                .defaultMetrics(new DefaultMetrics(plugin)::append)
                .onReady(context -> {
                    if (!plugin.isEnabled()) throw new IllegalStateException(
                            "The FastStats context of " + plugin.getName() + " must be made ready in or after onEnable"
                    );
                    if (config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                    registerPaperListener(plugin, context);
                });
    }

    private static void registerPaperListener(final Plugin plugin, final FastStatsContext context) {
        try {
            Class.forName("com.destroystokyo.paper.event.server.ServerExceptionEvent");
            plugin.getServer().getPluginManager().registerEvents(new PaperEventListener(plugin, context), plugin);
        } catch (final ClassNotFoundException ignored) {
        }
    }

    private static SimpleContext.Scheduler scheduler(final Plugin plugin) {
        return (task, initialDelay, period, unit) -> {
            try {
                return plugin.getServer().getAsyncScheduler().runAtFixedRate(
                        plugin, ignored -> task.run(), initialDelay, period, unit
                )::cancel;
            } catch (final IllegalPluginAccessException e) {
                throw e;
            } catch (final Throwable t) {
                return plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                        plugin, task, toTicks(initialDelay, unit), toTicks(period, unit)
                )::cancel;
            }
        };
    }

    private static final class DefaultMetrics {
        private final Plugin plugin;

        private final String minecraftVersion;
        private final String pluginVersion;
        private final String serverType;
        private final String serverVersion;

        @SuppressWarnings({"deprecation", "Convert2MethodRef"})
        private DefaultMetrics(final Plugin plugin) {
            this.plugin = plugin;
            final var server = plugin.getServer();

            this.serverVersion = server.getVersion().split("\\(MC: |\\)", 2)[0].strip();
            this.pluginVersion = tryOrEmpty(() -> plugin.getPluginMeta().getVersion())
                    .orElseGet(() -> plugin.getDescription().getVersion());
            this.minecraftVersion = tryOrEmpty(() -> server.getMinecraftVersion())
                    .or(() -> tryOrEmpty(() -> server.getBukkitVersion().split("-", 2)[0]))
                    .or(() -> tryOrEmpty(() -> server.getVersion().split("\\(MC: |\\)", 3)[1]))
                    .orElse(serverVersion);
            this.serverType = server.getName();
        }

        private void append(final SimpleContext context, final JsonObject metrics) {
            metrics.addProperty("game_version", minecraftVersion);
            metrics.addProperty("online_mode", checkOnlineMode());
            metrics.addProperty("player_count", getPlayerCount(context));
            metrics.addProperty("plugin_version", pluginVersion);
            metrics.addProperty("server_type", serverType);
            metrics.addProperty("platform_version", serverVersion);
        }

        private boolean checkOnlineMode() {
            final var server = plugin.getServer();
            return tryOrEmpty(() -> server.getServerConfig().isProxyOnlineMode())
                    .or(() -> tryOrEmpty(this::isProxyOnlineMode))
                    .orElseGet(server::getOnlineMode);
        }

        @SuppressWarnings("removal")
        private boolean isProxyOnlineMode() {
            final var server = plugin.getServer();
            final var proxies = server.spigot().getPaperConfig().getConfigurationSection("proxies");
            if (proxies == null) return false;

            if (proxies.getBoolean("velocity.enabled") && proxies.getBoolean("velocity.online-mode")) return true;

            final var settings = server.spigot().getSpigotConfig().getConfigurationSection("settings");
            if (settings == null) return false;

            return settings.getBoolean("bungeecord") && proxies.getBoolean("bungee-cord.online-mode");
        }

        private int getPlayerCount(final SimpleContext context) {
            try {
                return plugin.getServer().getOnlinePlayers().size();
            } catch (final Throwable t) {
                context.getLoggerFactory().getLogger(FastStatsBukkit.class).error("Failed to get player count", t);
                context.errorTrackerService().ifPresent(service -> service.globalErrorTracker().trackError(t));
                return 0;
            }
        }
    }

    private static <T> Optional<T> tryOrEmpty(final Supplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (final NoSuchMethodError | Exception e) {
            return Optional.empty();
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

    private static long toTicks(final long time, final TimeUnit unit) {
        return Math.max(1L, unit.toMillis(time) / 50L);
    }
}
