package dev.faststats.sponge;

import dev.faststats.Config;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public record SpongeConfig(
        UUID serverId,
        boolean enabled,
        boolean additionalMetrics,
        boolean debug,
        boolean submitMetrics,
        boolean errorTracking,
        boolean firstRun
) implements Config {
    private static volatile boolean loggingEnabled;
    private static final int CONFIG_VERSION = 2;

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

    @Contract(mutates = "io")
    public static SpongeConfig read(final PluginContainer plugin, final Path file, final LoggerFactory loggerFactory) throws RuntimeException {
        final var logger = loggerFactory.getLogger(SpongeConfig.class);
        final var debugFlag = Boolean.getBoolean("faststats.debug");

        final var properties = readOrEmpty(file);
        final var firstRun = properties == null;
        final var saveConfig = new AtomicBoolean(firstRun);

        final var serverId = parse(properties, saveConfig, "serverId", UUID::randomUUID, value -> {
            final var corrected = value.length() > 36 ? value.substring(0, 36) : value;
            final var uuid = UUID.fromString(corrected);
            if (!value.equals(uuid.toString())) saveConfig.set(true);
            return uuid;
        }, logger);
        final var configVersion = parse(properties, saveConfig, "configVersion", null, Integer::parseInt, logger);
        final boolean submitMetrics = parse(properties, saveConfig, "submitMetrics", () -> true, Boolean::parseBoolean, logger);
        final boolean errorTracking = parse(properties, saveConfig, "submitErrors", () -> true, Boolean::parseBoolean, logger);
        final boolean additionalMetrics = parse(properties, saveConfig, "submitAdditionalMetrics", () -> true, Boolean::parseBoolean, logger);
        final boolean debug = parse(properties, saveConfig, "debug", () -> true, Boolean::parseBoolean, logger);

        if (configVersion == null || configVersion < CONFIG_VERSION) saveConfig.set(true);
        else if (configVersion > CONFIG_VERSION) saveConfig.set(false);

        if (saveConfig.get()) try {
            if (configVersion != null && configVersion < CONFIG_VERSION)
                logger.info("Updating config version from %s to %s", configVersion, CONFIG_VERSION);
            Files.createDirectories(file.getParent());
            try (final var out = Files.newOutputStream(file);
                 final var writer = new OutputStreamWriter(out, UTF_8)) {
                final var store = new Properties();

                store.setProperty("submitMetrics", Boolean.toString(submitMetrics));
                store.setProperty("submitAdditionalMetrics", Boolean.toString(additionalMetrics));
                store.setProperty("submitErrors", Boolean.toString(errorTracking));

                store.setProperty("serverId", serverId.toString());

                store.setProperty("debug", Boolean.toString(debug));
                store.setProperty("configVersion", Integer.toString(CONFIG_VERSION));

                store.store(writer, COMMENT);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
        }

        final var enabled = Sponge.metricsConfigManager().effectiveCollectionState(plugin).asBoolean();
        return new SpongeConfig(
                serverId,
                enabled,
                enabled && additionalMetrics,
                debug || debugFlag,
                enabled && submitMetrics,
                enabled && errorTracking,
                firstRun
        );
    }

    @Contract(value = "_, _, _, !null, _, _ -> !null")
    private static <T> @Nullable T parse(
            @Nullable final Properties properties,
            final AtomicBoolean saveConfig,
            final String key,
            @Nullable final Supplier<T> defaultValue,
            final Function<String, T> parser,
            final Logger logger
    ) {
        if (properties == null) {
            saveConfig.set(true);
            return defaultValue != null ? defaultValue.get() : null;
        }
        final var property = properties.getProperty(key);
        if (property == null) {
            logger.warn("Missing configuration property: %s", key);
            saveConfig.set(true);
            return defaultValue != null ? defaultValue.get() : null;
        }
        try {
            return parser.apply(property.trim());
        } catch (final Exception e) {
            logger.error("Failed to read property '%s' from config", e, key);
            saveConfig.set(true);
            return defaultValue != null ? defaultValue.get() : null;
        }
    }

    private static @Nullable Properties readOrEmpty(final Path file) throws RuntimeException {
        if (!Files.isRegularFile(file)) return null;
        try (final var reader = Files.newBufferedReader(file, UTF_8)) {
            final var properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read metrics config", e);
        }
    }

    public boolean preSubmissionStart(final LoggerFactory loggerFactory) {
        if (Boolean.getBoolean("faststats.first-run")) return false;

        if (firstRun()) {
            var separatorLength = 0;
            final var split = ONBOARDING_MESSAGE.split("\n");
            for (final var s : split) if (s.length() > separatorLength) separatorLength = s.length();

            final var logger = loggerFactory.getLogger(getClass());
            logger.print(Logger.LogLevel.INFO, null, "-".repeat(separatorLength));
            for (final var s : split) logger.print(Logger.LogLevel.INFO, null, s);
            logger.print(Logger.LogLevel.INFO, null, "-".repeat(separatorLength));

            System.setProperty("faststats.first-run", "true");
            return false;
        }
        return true;
    }
}
