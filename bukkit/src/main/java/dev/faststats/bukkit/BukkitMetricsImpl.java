package dev.faststats.bukkit;

import com.google.gson.JsonObject;
import dev.faststats.ErrorTracker;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import dev.faststats.data.Metric;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.function.Supplier;

final class BukkitMetricsImpl extends SimpleMetrics implements BukkitMetrics {
    private final Plugin plugin;

    private final String pluginVersion;
    private final String minecraftVersion;
    private final String serverType;

    @Async.Schedule
    @Contract(mutates = "io")
    @SuppressWarnings({"deprecation", "Convert2MethodRef"})
    private BukkitMetricsImpl(final Factory factory, final Plugin plugin) throws IllegalStateException {
        super(factory);

        this.plugin = plugin;
        final var server = plugin.getServer();

        this.pluginVersion = tryOrEmpty(() -> plugin.getPluginMeta().getVersion())
                .orElseGet(() -> plugin.getDescription().getVersion());
        this.minecraftVersion = tryOrEmpty(() -> server.getMinecraftVersion())
                .or(() -> tryOrEmpty(() -> server.getBukkitVersion().split("-", 2)[0]))
                .orElseGet(() -> server.getVersion().split("\\(MC: |\\)", 3)[1]);
        this.serverType = server.getName();

        startSubmitting();
    }

    Plugin plugin() {
        return plugin;
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

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) context.getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("minecraft_version", minecraftVersion);
        metrics.addProperty("online_mode", checkOnlineMode());
        metrics.addProperty("player_count", getPlayerCount());
        metrics.addProperty("plugin_version", pluginVersion);
        metrics.addProperty("server_type", serverType);
    }

    private int getPlayerCount() {
        try {
            return plugin.getServer().getOnlinePlayers().size();
        } catch (final Throwable t) {
            logger.error("Failed to get player count", t);
            return 0;
        }
    }

    @Override
    public void ready() {
        if (getErrorTracker().isPresent()) try {
            Class.forName("com.destroystokyo.paper.event.server.ServerExceptionEvent");
            plugin.getServer().getPluginManager().registerEvents(new PaperEventListener(this), plugin);
        } catch (final ClassNotFoundException ignored) {
        }
    }

    private <T> Optional<T> tryOrEmpty(final Supplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (final NoSuchMethodError | Exception e) {
            return Optional.empty();
        }
    }

    public static final class Factory extends SimpleMetrics.Factory implements BukkitMetrics.Factory {
        Factory(final BukkitContext context) {
            super(context);
        }

        @Override
        public Factory addMetric(final Metric<?> metric) throws IllegalArgumentException {
            return (Factory) super.addMetric(metric);
        }

        @Override
        public Factory onFlush(final Runnable flush) {
            return (Factory) super.onFlush(flush);
        }

        @Override
        public Factory errorTracker(final ErrorTracker tracker) {
            return (Factory) super.errorTracker(tracker);
        }

        @Override
        public BukkitMetrics create() throws IllegalStateException {
            return new BukkitMetricsImpl(this, ((BukkitContext) context).plugin);
        }
    }
}
