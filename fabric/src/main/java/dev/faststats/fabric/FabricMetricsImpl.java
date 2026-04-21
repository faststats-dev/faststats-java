package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.Metrics;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

final class FabricMetricsImpl extends SimpleMetrics {
    private final ModContainer mod;

    private @Nullable MinecraftServer server;

    @Async.Schedule
    @Contract(mutates = "io")
    private FabricMetricsImpl(final Factory factory, final ModContainer mod) throws IllegalStateException {
        super(factory);

        this.mod = mod;

        ServerLifecycleEvents.SERVER_STARTED.register(server -> { // todo: client support
            this.server = server;
            startSubmitting();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutdown());
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
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

    static final class Factory extends SimpleMetrics.Factory {
        public Factory(final FabricContext context) {
            super(context);
        }

        @Override
        public Metrics create() throws IllegalStateException, IllegalArgumentException {
            return new FabricMetricsImpl(this, ((FabricContext) context).mod);
        }
    }
}
