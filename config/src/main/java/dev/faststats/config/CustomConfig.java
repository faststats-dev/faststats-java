package dev.faststats.config;

import dev.faststats.Config;
import dev.faststats.SimpleContext;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A builder-style metrics configuration.
 * <p>
 * Implementations can override the header comment and the onboarding message,
 * and register additional fields that are stored alongside the default ones.
 *
 * <pre>{@code
 * public static final CustomConfig.Field<Boolean> ONBOARDING_SCREEN_SEEN =
 *         CustomConfig.Field.bool("onboarding-screen-seen", false);
 *
 * final var config = CustomConfig.builder(file, loggerFactory)
 *         .comment("My custom header")
 *         .onboardingMessage(null) // disable the onboarding message
 *         .field(ONBOARDING_SCREEN_SEEN)
 *         .build();
 *
 * if (!config.get(ONBOARDING_SCREEN_SEEN)) {
 *     config.set(ONBOARDING_SCREEN_SEEN, true);
 *     config.persist();
 * }
 * }</pre>
 *
 * @since 0.32.0
 */
public final class CustomConfig implements Config {
    private static final int CURRENT_CONFIG_VERSION = 3;

    public static final Field<Boolean> ENABLED = Field.bool("enabled", true);
    public static final Field<Boolean> SUBMIT_METRICS = Field.bool("submitMetrics", true);
    public static final Field<Boolean> SUBMIT_ADDITIONAL_METRICS = Field.bool("submitAdditionalMetrics", true);
    public static final Field<Boolean> SUBMIT_ERRORS = Field.bool("submitErrors", true);
    public static final Field<UUID> SERVER_ID = new Field<>("serverId", UUID::randomUUID,
            value -> UUID.fromString(value.length() > 36 ? value.substring(0, 36) : value), UUID::toString);
    public static final Field<Boolean> DEBUG = Field.bool("debug", false);
    private static final Field<Integer> CONFIG_VERSION = Field.integer("configVersion", CURRENT_CONFIG_VERSION);

    private static final Set<Field<?>> DEFAULT_FIELDS = Set.of(
            ENABLED, SUBMIT_METRICS, SUBMIT_ADDITIONAL_METRICS, SUBMIT_ERRORS, SERVER_ID, DEBUG, CONFIG_VERSION
    );

    public static final String DEFAULT_COMMENT = """
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
    public static final String DEFAULT_ONBOARDING_MESSAGE = """
            This plugin uses FastStats to collect pseudonymous usage statistics and errors.
            No personal or identifying information is ever collected.
            To opt out, set 'enabled=false' in the metrics configuration file.
            Learn more at: https://faststats.dev/info
            
            Since this is your first start with FastStats, submission will not start
            until after a restart, to allow you to opt out if you prefer.""";

    private final Path file;
    private final @Nullable String comment;
    private final @Nullable String onboardingMessage;
    private final Map<String, Field<?>> fields;
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final boolean storeEnabled;
    private final boolean firstRun;

    private CustomConfig(final Builder builder) {
        this.file = builder.file;
        this.comment = builder.comment;
        this.onboardingMessage = builder.onboardingMessage;
        this.fields = Map.copyOf(builder.fields);
        this.storeEnabled = builder.enabled == null;

        final var logger = builder.loggerFactory.getLogger(CustomConfig.class);

        final var properties = readOrEmpty(file);
        this.firstRun = properties == null;
        final var reader = new Reader(properties, logger);
        fields.values().forEach(field -> {
            if (field != ENABLED || storeEnabled) values.put(field.key(), reader.parse(field));
        });
        if (builder.enabled != null) values.put(ENABLED.key(), builder.enabled);

        final int configVersion = get(CONFIG_VERSION);
        if (configVersion < CURRENT_CONFIG_VERSION) {
            if (!reader.missing(CONFIG_VERSION))
                logger.info("Updating config version from %s to %s", configVersion, CURRENT_CONFIG_VERSION);
            set(CONFIG_VERSION, CURRENT_CONFIG_VERSION);
            reader.save = true;
        } else if (configVersion > CURRENT_CONFIG_VERSION) reader.save = false;

        if (reader.save) {
            final var store = new Properties();
            storeFields(store);
            write(store);
        }

        final var enabled = get(ENABLED) && enabledFlag();
        set(ENABLED, enabled);
        set(SUBMIT_METRICS, enabled && get(SUBMIT_METRICS));
        set(SUBMIT_ERRORS, enabled && get(SUBMIT_ERRORS));
        set(SUBMIT_ADDITIONAL_METRICS, enabled && get(SUBMIT_ADDITIONAL_METRICS));
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static Builder builder(final Path file, final LoggerFactory loggerFactory) {
        return new Builder(file, loggerFactory);
    }

    @Override
    public UUID serverId() {
        return get(SERVER_ID);
    }

    @Override
    public boolean enabled() {
        return get(ENABLED);
    }

    public void enabled(final boolean enabled) {
        set(ENABLED, enabled);
    }

    @Override
    public boolean additionalMetrics() {
        return get(SUBMIT_ADDITIONAL_METRICS);
    }

    public void additionalMetrics(final boolean additionalMetrics) {
        set(SUBMIT_ADDITIONAL_METRICS, additionalMetrics);
    }

    @Override
    public boolean debug() {
        return get(DEBUG) || Boolean.getBoolean("faststats.debug");
    }

    @Override
    public boolean submitMetrics() {
        return get(SUBMIT_METRICS);
    }

    public void submitMetrics(final boolean submitMetrics) {
        set(SUBMIT_METRICS, submitMetrics);
    }

    @Override
    public boolean errorTracking() {
        return get(SUBMIT_ERRORS);
    }

    public void errorTracking(final boolean errorTracking) {
        set(SUBMIT_ERRORS, errorTracking);
    }

    public boolean firstRun() {
        return firstRun;
    }

    /**
     * @return {@code false} if FastStats was disabled using {@code -Dfaststats.enabled=false}
     */
    public static boolean enabledFlag() {
        return Boolean.parseBoolean(System.getProperty("faststats.enabled", "true"));
    }

    /**
     * Whether the given field is registered in this config.
     *
     * @param field the field
     * @return {@code true} if the field is registered, {@code false} otherwise
     * @since 0.32.0
     */
    @Contract(pure = true)
    public boolean has(final Field<?> field) {
        return field.equals(fields.get(field.key()));
    }

    /**
     * Gets the value of a field.
     *
     * @param field the field
     * @param <T>   the value type
     * @return the current value
     * @throws IllegalArgumentException if the field was not registered with the builder
     * @since 0.32.0
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Field<T> field) throws IllegalArgumentException {
        requireRegistered(field);
        return (T) values.get(field.key());
    }

    /**
     * Sets the value of a field.
     * <p>
     * Call {@link #persist()} to write the change to disk.
     *
     * @param field the field
     * @param value the new value
     * @param <T>   the value type
     * @throws IllegalArgumentException if the field was not registered with the builder
     * @since 0.32.0
     */
    public <T> void set(final Field<T> field, final T value) throws IllegalArgumentException {
        requireRegistered(field);
        values.put(field.key(), Objects.requireNonNull(value, "value"));
    }

    private void requireRegistered(final Field<?> field) {
        if (!has(field))
            throw new IllegalArgumentException("Field '" + field.key() + "' is not registered in this config");
    }

    @Contract(mutates = "io")
    public synchronized void persist() throws RuntimeException {
        final var existing = readOrEmpty(file);
        final var properties = existing != null ? existing : new Properties();
        storeFields(properties);
        write(properties);
    }

    @SuppressWarnings("unchecked")
    private void storeFields(final Properties properties) {
        fields.values().forEach(field -> {
            if (field == ENABLED && !storeEnabled) return;
            final var typed = (Field<Object>) field;
            properties.setProperty(field.key(), typed.serializer().apply(values.get(field.key())));
        });
    }

    private void write(final Properties properties) throws RuntimeException {
        try {
            Files.createDirectories(file.getParent());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create metrics config directory", e);
        }
        try (final var writer = Files.newBufferedWriter(file, UTF_8)) {
            properties.store(writer, comment);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
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
        if (!firstRun()) return true;

        if (onboardingMessage != null) {
            var separatorLength = 0;
            final var split = onboardingMessage.split("\n");
            for (final var s : split) if (s.length() > separatorLength) separatorLength = s.length();

            final var logger = context.getLoggerFactory().getLogger(getClass());
            logger.print(Logger.LogLevel.INFO, null, "-".repeat(separatorLength));
            for (final var s : split) logger.print(Logger.LogLevel.INFO, null, s);
            logger.print(Logger.LogLevel.INFO, null, "-".repeat(separatorLength));
        }

        System.setProperty("faststats.first-run", "true");
        return false;
    }

    @Override
    public String toString() {
        return "CustomConfig{" +
                "firstRun=" + firstRun +
                ", values=" + values +
                '}';
    }

    private static final class Reader {
        private final @Nullable Properties properties;
        private final Logger logger;
        private boolean save;

        private Reader(@Nullable final Properties properties, final Logger logger) {
            this.properties = properties;
            this.logger = logger;
            this.save = properties == null;
        }

        private boolean missing(final Field<?> field) {
            return properties == null || properties.getProperty(field.key()) == null;
        }

        private <T> T parse(final Field<T> field) {
            if (properties == null) return field.defaultValue().get();
            final var property = properties.getProperty(field.key());
            if (property == null) {
                logger.warn("Missing configuration property: %s", field.key());
                save = true;
                return field.defaultValue().get();
            }
            try {
                final var trimmed = property.trim();
                final var value = field.parser().apply(trimmed);
                if (!trimmed.equals(field.serializer().apply(value))) save = true;
                return value;
            } catch (final Exception e) {
                logger.error("Failed to read property '%s' from config", e, field.key());
                save = true;
                return field.defaultValue().get();
            }
        }
    }

    /**
     * Builder for {@link CustomConfig}.
     *
     * @since 0.32.0
     */
    public static final class Builder {
        private final Path file;
        private final LoggerFactory loggerFactory;
        private final Map<String, Field<?>> fields = new LinkedHashMap<>();
        private @Nullable String comment = DEFAULT_COMMENT;
        private @Nullable String onboardingMessage = DEFAULT_ONBOARDING_MESSAGE;
        private @Nullable Boolean enabled;

        private Builder(final Path file, final LoggerFactory loggerFactory) {
            this.file = file;
            this.loggerFactory = loggerFactory;
            DEFAULT_FIELDS.forEach(field -> fields.put(field.key(), field));
        }

        /**
         * Sets the header comment of the config file.
         * <p>
         * Lines are prefixed with {@code #} automatically.
         *
         * @param comment the comment, or {@code null} to omit it
         * @return this builder
         * @since 0.32.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder comment(@Nullable final String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets the message logged on the first run.
         *
         * @param onboardingMessage the message, or {@code null} to disable it
         * @return this builder
         * @since 0.32.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder onboardingMessage(@Nullable final String onboardingMessage) {
            this.onboardingMessage = onboardingMessage;
            return this;
        }

        /**
         * Overrides the enabled state with one provided by the platform.
         * <p>
         * The {@code enabled} entry is then neither read from nor written to the config file.
         *
         * @param enabled the enabled state
         * @return this builder
         * @since 0.32.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Registers an additional field.
         *
         * @param field the field
         * @return this builder
         * @throws IllegalArgumentException if the key is already registered
         * @since 0.32.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder field(final Field<?> field) throws IllegalArgumentException {
            if (fields.putIfAbsent(field.key(), field) != null)
                throw new IllegalArgumentException("Field key '" + field.key() + "' is already registered");
            return this;
        }

        /**
         * Reads the config from disk, creating or updating the file if necessary.
         *
         * @return the config
         * @throws RuntimeException if the file could not be read or written
         * @since 0.32.0
         */
        @Contract(value = " -> new", mutates = "io")
        public CustomConfig build() throws RuntimeException {
            return new CustomConfig(this);
        }
    }

    /**
     * A config field.
     *
     * @param key          the property key
     * @param defaultValue the default value supplier
     * @param parser       converts the stored string into the value
     * @param serializer   converts the value into the stored string
     * @param <T>          the value type
     * @since 0.32.0
     */
    public record Field<T>(
            String key,
            Supplier<T> defaultValue,
            Function<String, T> parser,
            Function<T, String> serializer
    ) {
        @Contract(value = "_, _ -> new", pure = true)
        public static Field<Boolean> bool(final String key, final boolean defaultValue) {
            return new Field<>(key, () -> defaultValue, Boolean::parseBoolean, String::valueOf);
        }

        @Contract(value = "_, _ -> new", pure = true)
        public static Field<Integer> integer(final String key, final int defaultValue) {
            return new Field<>(key, () -> defaultValue, Integer::parseInt, String::valueOf);
        }

        @Contract(value = "_, _ -> new", pure = true)
        public static Field<String> string(final String key, final String defaultValue) {
            return new Field<>(key, () -> defaultValue, Function.identity(), Function.identity());
        }
    }
}
