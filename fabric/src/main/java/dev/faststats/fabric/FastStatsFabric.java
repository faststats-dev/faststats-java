package dev.faststats.fabric;

import com.google.gson.JsonObject;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.config.SimpleConfig;
import dev.faststats.fabric.compat.CompatibilityLayer;
import dev.faststats.internal.PlatformLoggerFactory;
import dev.faststats.screen.registry.FastStatsRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * FastStats for Fabric and Quilt mods.
 * <p>
 * Build the context in your mod initializer:
 * <pre>{@code
 * @Override
 * public void onInitialize() {
 *     this.context = FastStatsFabric.builder("example-mod")
 *             .requireConsent() // optional, shows the consent screen on the client
 *             .metrics(Metrics.Factory::create)
 *             .errorTrackerService(ErrorTracker.contextAware())
 *             .build();
 * }
 * }</pre>
 * <p>
 * The lifecycle of the built context is managed automatically,
 * you neither have to make it {@linkplain FastStatsContext#ready() ready} nor shut it down:
 * on a client it is ready right away and shut down when the client stops,
 * on a dedicated server it is ready once the server started and shut down when it stops.
 *
 * @since 0.33.0
 */
public final class FastStatsFabric {
    private FastStatsFabric() {
    }

    /**
     * Creates a new context builder for the given mod.
     * <p>
     * The shared FastStats config is read from {@code config/faststats/config.properties} right away.
     * If no {@linkplain FastStatsContext.Builder#token(String) token} is set, the token injected at build time
     * is read from {@code META-INF/faststats/<mod-id>.token}.
     * If no token was injected, a warning is logged and the built context is disabled.
     * <p>
     * By default, no consent is required. Use {@link FastStatsContext.ConsentBuilder#requireConsent()}
     * to show the consent screen on the client: when at least one mod requires consent,
     * the screen is opened until it was shown once, and nothing is submitted before that.
     * On a dedicated server, consent is never required.
     *
     * @param modId the mod id
     * @return a new context builder
     * @throws IllegalArgumentException if the mod is not loaded
     * @since 0.33.0
     */
    @Contract(value = "_ -> new", mutates = "io")
    public static FastStatsContext.ConsentBuilder builder(final String modId) throws IllegalArgumentException {
        final var logger = org.slf4j.LoggerFactory.getLogger(modId);
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
        final var environment = FabricLoader.getInstance().getEnvironmentType();
        final var client = environment == EnvType.CLIENT;
        final ModContainer mod = FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        final var compatibilityLayer = ServiceLoader.load(CompatibilityLayer.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Fabric compatibility layer found"));
        final var config = SimpleConfig.read(configPath(), loggerFactory, client);

        return new SimpleContext.ConsentBuilder("fabric")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(mod.getMetadata().getId())
                .tokenResource(FastStatsFabric.class.getClassLoader(), modId)
                .clientApplication(client)
                .defaultMetrics((ignored, metrics) -> {
                    metrics.addProperty("online_mode", client ? compatibilityLayer.clientOnlineMode() : compatibilityLayer.serverOnlineMode());
                    metrics.addProperty("player_count", client ? compatibilityLayer.clientPlayerCount() : compatibilityLayer.serverPlayerCount());
                    appendFabricData(metrics, mod, client);
                })
                .onReady(context -> {
                    // dedicated servers never ask for consent, clients start submission once built
                    if (!client && config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                })
                .onShutdown(FastStatsRegistry.instance()::unregister)
                .onBuild((context, builder) -> {
                    final var requireConsent = ((SimpleContext.ConsentBuilder) builder).isConsentRequired();
                    if (context.isEnabled()) FastStatsRegistry.instance().register(context, requireConsent);
                    switch (environment) {
                        case CLIENT -> {
                            context.ready();
                            // otherwise the consent screen starts submission once consent was given
                            if (config.preSubmissionStart(loggerFactory) && (!requireConsent || config.consentScreenShown()))
                                context.startSubmission();
                            ClientLifecycleEvents.CLIENT_STOPPING.register(ignored -> context.shutdown());
                        }
                        case SERVER -> {
                            // the server instance is captured once it is starting, only needed for metrics
                            if (context.metrics().isPresent()) compatibilityLayer.initServer();
                            ServerLifecycleEvents.SERVER_STARTED.register(ignored -> context.ready());
                            ServerLifecycleEvents.SERVER_STOPPING.register(ignored -> context.shutdown());
                        }
                    }
                });
    }

    private static void appendFabricData(final JsonObject metrics, final ModContainer mod, final boolean client) {
        final var loader = isQuilt() ? "Quilt" : "Fabric";
        version("minecraft").ifPresent(s -> metrics.addProperty("game_version", s));
        version(isQuilt() ? "quilt_loader" : "fabricloader").ifPresent(s -> metrics.addProperty("platform_version", s));
        metrics.addProperty("plugin_version", mod.getMetadata().getVersion().getFriendlyString());
        metrics.addProperty("server_type", client ? loader + " Client" : loader);
    }

    private static boolean isQuilt() {
        return FabricLoader.getInstance().isModLoaded("quilt_loader");
    }

    private static Optional<String> version(final String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("faststats").resolve("config.properties");
    }
}
