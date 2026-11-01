package dev.faststats.config;

import dev.faststats.Config;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

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
    private final Logger logger;
    private final @Nullable String comment;
    private final @Nullable String onboardingMessage;
    private final Map<String, Field<?>> fields;
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final boolean storeEnabled;
    private final boolean firstRun;

    private CustomConfig(final Builder builder) {
        this.file = builder.file;
        this.logger = builder.loggerFactory.getLogger(CustomConfig.class);
        this.comment = builder.comment;
        this.onboardingMessage = builder.onboardingMessage;
        this.fields = Map.copyOf(builder.fields);
        this.storeEnabled = builder.enabled == null;

        PropertiesFile.Contents contents;
        var broken = false;
        try {
            contents = PropertiesFile.read(file);
        } catch (final IOException | RuntimeException e) {
            logger.print(Logger.LogLevel.ERROR, e, "Failed to read FastStats config at " + file
                    + ", FastStats is disabled until the file is fixed or deleted");
            contents = null;
            broken = true;
        }
        final var properties = contents != null ? contents.properties() : null;

        final var reader = new Reader(properties, fields.values(), logger);
        this.firstRun = !broken && reader.empty;
        fields.values().forEach(field -> {
            if (field != ENABLED || storeEnabled) values.put(field.key(), reader.parse(field));
        });
        if (builder.enabled != null) values.put(ENABLED.key(), builder.enabled);
        if (broken) values.put(ENABLED.key(), false);
        if (contents != null && contents.foreignEncoding()) reader.save = true;

        final int configVersion = get(CONFIG_VERSION);
        if (configVersion < CURRENT_CONFIG_VERSION) {
            if (reader.find(CONFIG_VERSION) != null)
                logger.info("Updating config version from %s to %s", configVersion, CURRENT_CONFIG_VERSION);
            set(CONFIG_VERSION, CURRENT_CONFIG_VERSION);
            reader.save = true;
        } else if (configVersion > CURRENT_CONFIG_VERSION) reader.save = false;

        if (reader.save && !broken) write(properties != null ? properties : new Properties());

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

    /**
     * Writes the current values to the config file.
     * <p>
     * Entries that are not registered in this config are kept.
     * Failures are logged instead of thrown.
     */
    @Contract(mutates = "io")
    public synchronized void persist() {
        PropertiesFile.Contents existing;
        try {
            existing = PropertiesFile.read(file);
        } catch (final IOException | RuntimeException e) {
            existing = null;
        }
        write(existing != null ? existing.properties() : new Properties());
    }

    @SuppressWarnings("unchecked")
    private void write(final Properties properties) {
        fields.values().forEach(field -> {
            if (field == ENABLED && !storeEnabled) return;
            final var typed = (Field<Object>) field;
            try {
                properties.setProperty(field.key(), typed.serializer().apply(values.get(field.key())));
                PropertiesFile.removeVariants(properties, field.key());
            } catch (final RuntimeException e) {
                logger.print(Logger.LogLevel.WARN, e, "Failed to serialize FastStats config property '" + field.key() + "'");
            }
        });
        try {
            PropertiesFile.write(file, properties, comment);
        } catch (final IOException | RuntimeException e) {
            logger.print(Logger.LogLevel.WARN, e, "Failed to save FastStats config at " + file
                    + ", changes will not persist across restarts");
        }
    }

    public boolean preSubmissionStart(final LoggerFactory loggerFactory) {
        if (Boolean.getBoolean("faststats.first-run")) return false;
        if (!firstRun()) return true;

        if (onboardingMessage != null) {
            var separatorLength = 0;
            final var split = onboardingMessage.split("\n");
            for (final var s : split) if (s.length() > separatorLength) separatorLength = s.length();

            final var logger = loggerFactory.getLogger(getClass());
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
        private final boolean empty;
        private boolean save;

        private Reader(@Nullable final Properties properties, final Collection<Field<?>> fields, final Logger logger) {
            this.properties = properties;
            this.logger = logger;
            this.empty = properties == null || fields.stream().allMatch(field -> find(field) == null);
            this.save = empty;
        }

        private @Nullable String find(final Field<?> field) {
            return properties != null ? PropertiesFile.findKey(properties, field.key()) : null;
        }

        private <T> T parse(final Field<T> field) {
            if (properties == null) return field.defaultValue().get();
            final var key = find(field);
            if (key == null) {
                if (!empty) logger.print(Logger.LogLevel.WARN, null, "Missing FastStats config property '"
                        + field.key() + "', using the default value");
                save = true;
                return field.defaultValue().get();
            }
            if (!key.equals(field.key())) save = true;
            final var property = PropertiesFile.clean(properties.getProperty(key));
            try {
                final var value = field.parser().apply(property);
                if (!property.equals(field.serializer().apply(value))) save = true;
                return value;
            } catch (final Exception e) {
                logger.print(Logger.LogLevel.WARN, null, "Invalid value '" + property
                        + "' for FastStats config property '" + field.key() + "', using the default value");
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
            final var normalized = PropertiesFile.normalize(field.key());
            if (fields.keySet().stream().map(PropertiesFile::normalize).anyMatch(normalized::equals))
                throw new IllegalArgumentException("Field key '" + field.key() + "' is already registered");
            fields.put(field.key(), field);
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
     * @param key          the property key, ASCII letters and digits optionally separated by single {@code -}, {@code _} or {@code .}
     * @param defaultValue the default value supplier
     * @param parser       converts the stored string into the value
     * @param serializer   converts the value into the stored string
     * @param <T>          the value type
     * @since 0.32.0
     */
    @SuppressWarnings("PatternValidation")
    public record Field<T>(
            @Pattern(KEY_PATTERN) String key,
            Supplier<T> defaultValue,
            Function<String, T> parser,
            Function<T, String> serializer
    ) {
        private static final String KEY_PATTERN = "[A-Za-z0-9]+([._-][A-Za-z0-9]+)*";
        private static final java.util.regex.Pattern KEY = java.util.regex.Pattern.compile(KEY_PATTERN);

        /**
         * @throws IllegalArgumentException if the key does not match the key pattern
         */
        public Field {
            if (!KEY.matcher(key).matches()) throw new IllegalArgumentException("Invalid field key '" + key
                    + "', only ASCII letters and digits optionally separated by single '-', '_' or '.' are allowed");
        }

        @Contract(value = "_, _ -> new", pure = true)
        public static Field<Boolean> bool(@Pattern(KEY_PATTERN) final String key, final boolean defaultValue) {
            return new Field<>(key, () -> defaultValue, Boolean::parseBoolean, String::valueOf);
        }

        @Contract(value = "_, _ -> new", pure = true)
        public static Field<Integer> integer(@Pattern(KEY_PATTERN) final String key, final int defaultValue) {
            return new Field<>(key, () -> defaultValue, Integer::parseInt, String::valueOf);
        }

        @Contract(value = "_, _ -> new", pure = true)
        public static Field<String> string(@Pattern(KEY_PATTERN) final String key, final String defaultValue) {
            return new Field<>(key, () -> defaultValue, Function.identity(), Function.identity());
        }
    }
}
