package dev.faststats.sponge;

import dev.faststats.config.CustomConfig;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;

@ApiStatus.Internal
final class SpongeConfig {
    private static final String COMMENT = """
             FastStats (https://faststats.dev) collects anonymous usage statistics and errors.
            # This helps developers understand how their projects are used in the real world.
            #
            # No IP addresses, player data, or personal information is collected.
            # The server ID below is randomly generated and can be regenerated at any time.
            #
            # Enabling metrics has no noticeable performance impact.
            # Enabling metrics is highly recommended, you can do so in the Sponge metrics.config,
            # by setting the "global-state" property to "TRUE".
            # To disable only metrics submission, set 'submitMetrics=false'.
            # To disable additional metrics, set 'submitAdditionalMetrics=false'.
            # To disable error tracking, set 'submitErrors=false'.
            #
            # If you suspect a developer is collecting personal data or bypassing the Sponge config,
            # please report it at: https://faststats.dev/abuse
            #
            # For more information, visit: https://faststats.dev/info
            """;
    private static final String ONBOARDING_MESSAGE = """
            This plugin uses FastStats to collect anonymous usage statistics and errors.
            No personal or identifying information is ever collected.
            It is recommended to enable metrics by setting 'global-state=TRUE' in the sponge metrics config.
            Learn more at: https://faststats.dev/info
            
            Since this is your first start with FastStats, submission will not start
            until you restart the server to allow you to opt out if you prefer.
            """;

    private SpongeConfig() {
    }

    @Contract(mutates = "io")
    static CustomConfig read(final PluginContainer plugin, final Path file, final LoggerFactory loggerFactory) throws RuntimeException {
        return CustomConfig.builder(file, loggerFactory)
                .comment(COMMENT)
                .onboardingMessage(ONBOARDING_MESSAGE)
                .enabled(Sponge.metricsConfigManager().effectiveCollectionState(plugin).asBoolean())
                .build();
    }
}
