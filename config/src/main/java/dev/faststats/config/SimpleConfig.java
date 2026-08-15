package dev.faststats.config;

import dev.faststats.Config;
import dev.faststats.SimpleContext;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public final class SimpleConfig implements Config {
    private static final int CONFIG_VERSION = 3;

    private static final String COMMENT = """
             FastStats (https://faststats.dev) collects pseudonymous usage statistics and errors.
            # This helps developers understand how their projects are used in the real world.
            #
            # No IP addresses, player data, or personal information is collected.
            # The server ID below is randomly generated and can be regenerated at any time.
            #
            # Enabling metrics has no noticeable performance impact.
            # Keeping FastStats enabled is recommended.
            # To disable all FastStats features, set 'enabled=false'.
            # To disable only metrics submission, set 'submitMetrics=false'.
            # To disable only additional metrics, set 'submitAdditionalMetrics=false'.
            # To disable only error tracking, set 'submitErrors=false'.
            #
            # If you suspect a developer is collecting personal data or bypassing any opt-out option,
            # please report it at: https://faststats.dev/abuse
            #
            # For more information, visit: https://faststats.dev/info
            """;
    private static final String ONBOARDING_MESSAGE = """
            This plugin uses FastStats to collect pseudonymous usage statistics and errors.
            No personal or identifying information is ever collected.
            To opt out, set 'enabled=false' in the metrics configuration file.
            Learn more at: https://faststats.dev/info
            
            Since this is your first start with FastStats, submission will not start
            until after a restart, to allow you to opt out if you prefer.""";

    private final Path file;
    private final UUID serverId;
    private final boolean debug;
    private final boolean firstRun;
    private volatile boolean additionalMetrics;
    private volatile boolean enabled;
    private volatile boolean errorTracking;
    private volatile boolean submitMetrics;

    public SimpleConfig(
            final Path file,
            final UUID serverId,
            final boolean enabled,
            final boolean additionalMetrics,
            final boolean debug,
            final boolean submitMetrics,
            final boolean errorTracking,
            final boolean firstRun
    ) {
        this.file = file;
        this.serverId = serverId;
        this.enabled = enabled;
        this.additionalMetrics = additionalMetrics;
        this.debug = debug;
        this.submitMetrics = submitMetrics;
        this.errorTracking = errorTracking;
        this.firstRun = firstRun;
    }

    @Override
    public UUID serverId() {
        return serverId;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    public void enabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean additionalMetrics() {
        return additionalMetrics;
    }

    public void additionalMetrics(final boolean additionalMetrics) {
        this.additionalMetrics = additionalMetrics;
    }

    @Override
    public boolean debug() {
        return debug;
    }

    @Override
    public boolean submitMetrics() {
        return submitMetrics;
    }

    public void submitMetrics(final boolean submitMetrics) {
        this.submitMetrics = submitMetrics;
    }

    @Override
    public boolean errorTracking() {
        return errorTracking;
    }

    public void errorTracking(final boolean errorTracking) {
        this.errorTracking = errorTracking;
    }

    public boolean firstRun() {
        return firstRun;
    }

    @Contract(mutates = "io")
    public static SimpleConfig read(final Path file, final LoggerFactory factory) throws RuntimeException {
        final var logger = factory.getLogger(SimpleConfig.class);

        final var debugFlag = Boolean.getBoolean("faststats.debug");
        final var enabledFlag = Boolean.parseBoolean(System.getProperty("faststats.enabled", "true"));

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
        final var enabled = parse(properties, saveConfig, "enabled", () -> true, Boolean::parseBoolean, logger);
        final var submitMetrics = parse(properties, saveConfig, "submitMetrics", () -> true, Boolean::parseBoolean, logger);
        final var errorTracking = parse(properties, saveConfig, "submitErrors", () -> true, Boolean::parseBoolean, logger);
        final var additionalMetrics = parse(properties, saveConfig, "submitAdditionalMetrics", () -> true, Boolean::parseBoolean, logger);
        final var debug = parse(properties, saveConfig, "debug", () -> false, Boolean::parseBoolean, logger);

        if (configVersion == null || configVersion < CONFIG_VERSION) saveConfig.set(true);
        else if (configVersion > CONFIG_VERSION) saveConfig.set(false);

        if (saveConfig.get()) try {
            if (configVersion != null && configVersion < CONFIG_VERSION)
                logger.info("Updating config version from %s to %s", configVersion, CONFIG_VERSION);
            Files.createDirectories(file.getParent());
            try (final var writer = Files.newBufferedWriter(file, UTF_8)) {
                final var store = new Properties();

                store.setProperty("enabled", Boolean.toString(enabled));
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

        return new SimpleConfig(
                file,
                serverId,
                enabled && enabledFlag,
                enabled && enabledFlag && additionalMetrics,
                debug || debugFlag,
                enabled && enabledFlag && submitMetrics,
                enabled && enabledFlag && errorTracking,
                firstRun
        );
    }

    @Contract(mutates = "io")
    public void persist() throws RuntimeException {
        final var properties = readOrEmpty(file);
        if (properties == null) throw new IllegalStateException("Metrics config has not been initialized");

        properties.setProperty("enabled", Boolean.toString(enabled()));
        properties.setProperty("submitAdditionalMetrics", Boolean.toString(additionalMetrics()));
        properties.setProperty("submitErrors", Boolean.toString(errorTracking()));
        properties.setProperty("submitMetrics", Boolean.toString(submitMetrics()));

        try (final var writer = Files.newBufferedWriter(file, UTF_8)) {
            properties.store(writer, COMMENT);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
        }
    }

    // fixme: this code sucks ass
    @Contract(value = "_, _, _, !null, _, _-> !null")
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

    public boolean preSubmissionStart(final SimpleContext context) {
        if (Boolean.getBoolean("faststats.first-run")) return false;

        if (firstRun()) {
            var separatorLength = 0;
            final var split = ONBOARDING_MESSAGE.split("\n");
            for (final var s : split) if (s.length() > separatorLength) separatorLength = s.length();

            final var logger = context.getLoggerFactory().getLogger(getClass());
            logger.print(Logger.LogLevel.INFO, null, "-".repeat(separatorLength));
            for (final var s : split) logger.print(Logger.LogLevel.INFO, null, s);
            logger.print(Logger.LogLevel.INFO, null, "-".repeat(separatorLength));

            System.setProperty("faststats.first-run", "true");
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) return true;
        if (!(object instanceof final SimpleConfig that)) return false;
        return enabled == that.enabled
                && additionalMetrics == that.additionalMetrics
                && debug == that.debug
                && submitMetrics == that.submitMetrics
                && errorTracking == that.errorTracking
                && firstRun == that.firstRun
                && Objects.equals(serverId, that.serverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverId, enabled, additionalMetrics, debug, submitMetrics, errorTracking, firstRun);
    }

    @Override
    public String toString() {
        return "SimpleConfig[serverId=" + serverId
                + ", enabled=" + enabled
                + ", additionalMetrics=" + additionalMetrics
                + ", debug=" + debug
                + ", submitMetrics=" + submitMetrics
                + ", errorTracking=" + errorTracking
                + ", firstRun=" + firstRun + ']';
    }
}
