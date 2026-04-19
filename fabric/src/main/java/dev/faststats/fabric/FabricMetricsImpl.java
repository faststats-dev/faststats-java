package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

final class FabricMetricsImpl extends SimpleMetrics implements FabricMetrics {
    private final ModContainer mod;

    private @Nullable MinecraftServer server;

    @Async.Schedule
    @Contract(mutates = "io")
    private FabricMetricsImpl(final Factory factory, final ModContainer mod, final Path config) throws IllegalStateException {
        super(factory, config);

        this.mod = mod;

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            startSubmitting();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutdown());
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        assert server != null : "Server not initialized";
        metrics.addProperty("minecraft_version", server.getServerVersion());
        metrics.addProperty("online_mode", server.usesAuthentication());
        metrics.addProperty("player_count", server.getPlayerCount());
        metrics.addProperty("plugin_version", mod.getMetadata().getVersion().getFriendlyString());
        metrics.addProperty("server_type", "Fabric");
    }

    private <T> Optional<T> tryOrEmpty(final Supplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (final NoSuchMethodError | Exception e) {
            return Optional.empty();
        }
    }

    static final class Factory extends SimpleMetrics.Factory<String, FabricMetrics.Factory> implements FabricMetrics.Factory {
        @Override
        public Metrics create(final String modId) throws IllegalStateException, IllegalArgumentException {
            final var fabric = FabricLoader.getInstance();
            final var mod = fabric.getModContainer(modId).orElseThrow(() -> {
                return new IllegalArgumentException("Mod not found: " + modId);
            });

            final var dataFolder = fabric.getConfigDir().resolve("faststats");
            final var config = dataFolder.resolve("config.properties");

            return new FabricMetricsImpl(this, mod, config);
        }
    }
}
