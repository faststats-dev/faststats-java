package dev.faststats.config;

import dev.faststats.Config;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.logging.Level;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public record SimpleConfig(
        UUID serverId,
        boolean additionalMetrics,
        boolean debug,
        boolean enabled,
        boolean errorTracking,
        boolean firstRun
) implements Config {

    public static final String COMMENT = """
             FastStats (https://faststats.dev) collects anonymous usage statistics.
            # This helps developers understand how their projects are used in the real world.
            #
            # No IP addresses, player data, or personal information is collected.
            # The server ID below is randomly generated and can be regenerated at any time.
            #
            # Enabling metrics has no noticeable performance impact.
            # Keeping metrics enabled is recommended, but you can opt out by setting 'enabled=false'.
            #
            # If you suspect a developer is collecting personal data or bypassing the "enabled" option,
            # please report it at: https://faststats.dev/abuse
            #
            # For more information, visit: https://faststats.dev/info
            """;
    private static final String ONBOARDING_MESSAGE = """
            This plugin uses FastStats to collect anonymous usage statistics.
            No personal or identifying information is ever collected.
            To opt out, set 'enabled=false' in the metrics configuration file.
            Learn more at: https://faststats.dev/info
            
            Since this is your first start with FastStats, metrics submission will not start
            until you restart the server to allow you to opt out if you prefer.""";

    @Contract(mutates = "io")
    public static SimpleConfig read(final Path file) throws RuntimeException {
        final var properties = readOrEmpty(file);
        final var firstRun = properties.isEmpty();
        final var saveConfig = new AtomicBoolean(firstRun);

        final var serverId = properties.map(object -> object.getProperty("serverId")).map(string -> {
            try {
                final var trimmed = string.trim();
                final var corrected = trimmed.length() > 36 ? trimmed.substring(0, 36) : trimmed;
                if (!corrected.equals(string)) saveConfig.set(true);
                return UUID.fromString(corrected);
            } catch (final IllegalArgumentException e) {
                saveConfig.set(true);
                return UUID.randomUUID();
            }
        }).orElseGet(() -> {
            saveConfig.set(true);
            return UUID.randomUUID();
        });

        final BiPredicate<String, Boolean> predicate = (key, defaultValue) -> {
            return properties.map(object -> object.getProperty(key)).map(Boolean::parseBoolean).orElseGet(() -> {
                saveConfig.set(true);
                return defaultValue;
            });
        };

        final var enabled = predicate.test("enabled", true);
        final var errorTracking = predicate.test("submitErrors", true);
        final var additionalMetrics = predicate.test("submitAdditionalMetrics", true);
        final var debug = predicate.test("debug", false);

        if (saveConfig.get()) try {
            Files.createDirectories(file.getParent());
            try (final var out = Files.newOutputStream(file);
                 final var writer = new OutputStreamWriter(out, UTF_8)) {
                final var store = new Properties();

                store.setProperty("serverId", serverId.toString());
                store.setProperty("enabled", Boolean.toString(enabled));
                store.setProperty("submitErrors", Boolean.toString(errorTracking));
                store.setProperty("submitAdditionalMetrics", Boolean.toString(additionalMetrics));
                store.setProperty("debug", Boolean.toString(debug));

                store.store(writer, COMMENT);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
        }

        return new SimpleConfig(serverId, additionalMetrics, debug, enabled, errorTracking, firstRun);
    }

    private static Optional<Properties> readOrEmpty(final Path file) throws RuntimeException {
        if (!Files.isRegularFile(file)) return Optional.empty();
        try (final var reader = Files.newBufferedReader(file, UTF_8)) {
            final var properties = new Properties();
            properties.load(reader);
            return Optional.of(properties);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read metrics config", e);
        }
    }

    @SuppressWarnings("PatternValidation")
    public boolean preSubmissionStart() {
        if (Boolean.getBoolean("faststats.first-run")) return false;

        if (firstRun()) {
            var separatorLength = 0;
            final var split = ONBOARDING_MESSAGE.split("\n");
            for (final var s : split) if (s.length() > separatorLength) separatorLength = s.length();

            final var logger = LoggerFactory.factory().getLogger(getClass());
            logger.log(Level.CONFIG, "-".repeat(separatorLength));
            for (final var s : split) logger.log(Level.CONFIG, s);
            logger.log(Level.CONFIG, "-".repeat(separatorLength));

            System.setProperty("faststats.first-run", "true");
            return false;
        }
        return true;
    }
}
