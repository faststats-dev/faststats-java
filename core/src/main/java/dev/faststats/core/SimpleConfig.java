package dev.faststats.core;

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

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public record SimpleConfig(
        UUID serverId,
        boolean additionalMetrics,
        boolean debug,
        boolean enabled,
        boolean errorTracking,
        boolean firstRun,
        boolean externallyManaged
) implements Config {

    public static final String DEFAULT_COMMENT = """
             FastStats (https://faststats.dev) collects anonymous usage statistics for developers.
            # This helps developers understand how their projects are used in the real world.
            #
            # No IP addresses, player data, or personal information is collected.
            # The server ID below is randomly generated and can be regenerated at any time.
            #
            # Enabling metrics has no noticeable performance impact.
            # Keeping metrics enabled is recommended, but you can opt out by setting
            # 'enabled=false' in faststats/config.properties.
            #
            # If you suspect a developer is collecting personal data or bypassing the "enabled" option,
            # please report it at: https://faststats.dev/abuse
            #
            # For more information, visit: https://faststats.dev/info
            """;

    @Contract(mutates = "io")
    public static SimpleConfig read(final Path file) throws RuntimeException {
        return read(file, DEFAULT_COMMENT, false, false);
    }

    @Contract(mutates = "io")
    public static SimpleConfig read(final Path file, final String comment, final boolean externallyManaged, final boolean externallyEnabled) throws RuntimeException {
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

        final var enabled = externallyManaged ? externallyEnabled : predicate.test("enabled", true);
        final var errorTracking = predicate.test("submitErrors", true);
        final var additionalMetrics = predicate.test("submitAdditionalMetrics", true);
        final var debug = predicate.test("debug", false);

        if (saveConfig.get()) try {
            save(file, externallyManaged, comment, serverId, enabled, errorTracking, additionalMetrics, debug);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
        }

        return new SimpleConfig(serverId, additionalMetrics, debug, enabled, errorTracking, firstRun, externallyManaged);
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

    private static void save(final Path file, final boolean externallyManaged, final String comment, final UUID serverId, final boolean enabled, final boolean errorTracking, final boolean additionalMetrics, final boolean debug) throws IOException {
        Files.createDirectories(file.getParent());
        try (final var out = Files.newOutputStream(file);
             final var writer = new OutputStreamWriter(out, UTF_8)) {
            final var properties = new Properties();

            properties.setProperty("serverId", serverId.toString());
            if (!externallyManaged) properties.setProperty("enabled", Boolean.toString(enabled));
            properties.setProperty("submitErrors", Boolean.toString(errorTracking));
            properties.setProperty("submitAdditionalMetrics", Boolean.toString(additionalMetrics));
            properties.setProperty("debug", Boolean.toString(debug));

            properties.store(writer, comment);
        }
    }
}
