package dev.faststats.forge;

import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.screen.onboarding.ConsentState;
import dev.faststats.forge.compat.CompatibilityLayer;
import dev.faststats.internal.PlatformLoggerFactory;
import dev.faststats.screen.registry.FastStatsRegistry;
import org.jetbrains.annotations.Contract;

import java.util.ServiceLoader;

/**
 * FastStats for Forge mods.
 * <p>
 * Build the context in your mod constructor:
 * <pre>{@code
 * public ExampleMod() {
 *     this.context = FastStatsForge.builder("example_mod")
 *             .requireConsent() // optional, shows the consent screen on the client
 *             .metrics(Metrics.Factory::create)
 *             .errorTrackerService(ErrorTracker.contextAware())
 *             .build();
 * }
 * }</pre>
 * <p>
 * The lifecycle of the built context is managed automatically,
 * you neither have to make it {@linkplain FastStatsContext#ready() ready} nor shut it down:
 * on a client it is ready right away,
 * on a dedicated server it is ready once the server started and shut down when it stops.
 *
 * @since 0.33.0
 */
public final class FastStatsForge {
    private FastStatsForge() {
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
                case INFO -> logger.info(message, throwable);
                case ERROR -> logger.error(message, throwable);
                case WARN -> logger.warn(message, throwable);
            }
        });
        final var compatibilityLayer = ServiceLoader.load(CompatibilityLayer.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Forge compatibility layer found"));
        final var client = compatibilityLayer.getEnvironment() == CompatibilityLayer.Environment.CLIENT;
        final var modVersion = compatibilityLayer.modVersion(modId).orElseThrow(() -> {
            return new IllegalArgumentException("Mod not found: " + modId);
        });
        final var config = ConsentState.readConfig(compatibilityLayer.configDirectory()
                .resolve("faststats").resolve("config.properties"), loggerFactory, client);

        return new SimpleContext.ConsentBuilder("forge")
                .loggerFactory(loggerFactory)
                .config(config)
                .projectName(modId)
                .tokenResource(FastStatsForge.class.getClassLoader(), modId)
                .clientApplication(client)
                .defaultMetrics((ignored, metrics) -> {
                    metrics.addProperty("online_mode", client ? compatibilityLayer.clientOnlineMode() : compatibilityLayer.serverOnlineMode());
                    metrics.addProperty("player_count", client ? compatibilityLayer.clientPlayerCount() : compatibilityLayer.serverPlayerCount());
                    compatibilityLayer.modVersion("minecraft").ifPresent(s -> metrics.addProperty("game_version", s));
                    compatibilityLayer.modVersion("forge").ifPresent(s -> metrics.addProperty("platform_version", s));
                    metrics.addProperty("plugin_version", modVersion);
                    metrics.addProperty("server_type", client ? "Forge Client" : "Forge");
                })
                .onReady(context -> {
                    // dedicated servers never ask for consent, clients start submission once built
                    if (!client && config.preSubmissionStart(loggerFactory)) ((SimpleContext) context).startSubmission();
                })
                .onShutdown(FastStatsRegistry.instance()::unregister)
                .onBuild((context, builder) -> {
                    final var requireConsent = ((SimpleContext.ConsentBuilder) builder).isConsentRequired();
                    if (context.isEnabled()) FastStatsRegistry.instance().register(context, requireConsent);
                    if (client) {
                        context.ready();
                        // otherwise the consent screen starts submission once consent was given
                        if (config.preSubmissionStart(loggerFactory) && (!requireConsent || ConsentState.consentScreenShown(config)))
                            context.startSubmission();
                    } else compatibilityLayer.onServerLifecycle(context::ready, context::shutdown);
                });
    }
}
