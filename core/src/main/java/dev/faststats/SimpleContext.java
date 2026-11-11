package dev.faststats;

import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public non-sealed abstract class SimpleContext implements FastStatsContext {
    private final @Token String token;
    private final boolean enabled;
    private final Config config;
    private final SdkInfo sdkInfo;

    private final LoggerFactory loggerFactory;
    private final Logger logger;

    protected volatile boolean ready = false;
    public volatile boolean submissionActive;

    private @Nullable Metrics metrics;
    private @Nullable FeatureFlagService featureFlagService;
    private @Nullable SimpleErrorTrackerService errorTrackerService;

    /**
     * Creates a new context that stores the shared configuration and token for all FastStats services.
     *
     * @param config the shared configuration
     * @param name   the name of the SDK
     * @param token  the FastStats project token, or {@code null} to create a disabled context without any services
     * @throws IllegalArgumentException if the token is invalid
     * @throws IllegalArgumentException if the SDK information is invalid
     * @throws IllegalStateException    if the SDK information is incomplete or missing
     * @throws UncheckedIOException     if an IO error occurs
     * @since 0.24.0
     */
    protected SimpleContext(final Factory<?, ?> factory, final LoggerFactory loggerFactory, final Config config, final String name, @Token final @Nullable String token) throws IllegalArgumentException {
        if (token != null && !token.matches(Token.PATTERN))
            throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");

        this.loggerFactory = loggerFactory;
        loggerFactory.setDebug(config.debug());
        this.logger = loggerFactory.getLogger(getClass());

        this.sdkInfo = constructSdkInfo(name);
        this.config = config;
        this.token = token != null ? token : "";
        this.enabled = token != null;
    }

    /**
     * Whether this context was created with a token.
     * <p>
     * A context without a token is disabled: it has no services and never submits any data.
     *
     * @return {@code true} if this context has a token
     * @since 0.32.1
     */
    @Contract(pure = true)
    protected final boolean isEnabled() {
        return enabled;
    }

    @MustBeInvokedByOverriders
    protected final void initializeServices(final Factory<?, ?> factory) throws IllegalStateException {
        initializeServices(factory, false);
    }

    /**
     * Initializes service descriptions before consent so this context can participate in a live lifecycle registry.
     */
    @MustBeInvokedByOverriders
    protected final void initializeManagedServices(final Factory<?, ?> factory) throws IllegalStateException {
        initializeServices(factory, true);
    }

    private void initializeServices(final Factory<?, ?> factory, final boolean lifecycleManaged) {
        if (factory.metrics == null && factory.errorTracker == null && factory.featureFlagService == null)
            throw new IllegalStateException("Context created without any service attached, was this intentional?");
        if (!enabled) {
            final var resource = factory.tokenResource != null ? factory.tokenResource : Factory.TOKEN_RESOURCE_DIRECTORY + "<id>.token";
            logger.warn("No valid FastStats token provided for %s, FastStats is disabled. Pass a token to the factory or inject it at build time into %s",
                    getProjectName(), resource);
            return;
        }

        final var start = preSubmissionStart();
        if (!lifecycleManaged && !start) return;

        if (factory.metrics != null && (lifecycleManaged || config.submitMetrics())) {
            final var metricsFactory = metricsFactory();
            this.metrics = factory.metrics.apply(metricsFactory);
        }
        this.errorTrackerService = factory.errorTracker != null && (lifecycleManaged || config.errorTracking())
                ? new SimpleErrorTrackerService(this, factory.errorTracker)
                : null;
        this.featureFlagService = factory.featureFlagService != null ? factory.featureFlagService.apply(new SimpleFeatureFlagService.Factory(this)) : null;
        this.submissionActive = start;

        final var features = new HashSet<String>(3);
        features.add("metrics=" + (metrics != null ? "yes" : "no"));
        features.add("error-tracking=" + (errorTrackerService != null ? "yes" : "no"));
        features.add("feature-flags=" + (featureFlagService != null ? "yes" : "no"));

        logger.info("Created FastStats context for %s using %s (%s)",
                getProjectName(), sdkInfo.getUserAgent(),
                String.join(", ", features)
        );
    }

    private SdkInfo constructSdkInfo(final String name) throws UncheckedIOException, IllegalStateException, IllegalArgumentException {
        try (final var stream = getClass().getResourceAsStream("/META-INF/faststats.properties")) {
            if (stream == null) throw new IllegalStateException("Resource '/META-INF/faststats.properties' not found");

            final var properties = new Properties();
            properties.load(stream);

            final var version = properties.getProperty("version", null);
            if (version == null) throw new IllegalStateException("Missing 'version' in faststats.properties");

            final var buildId = properties.getProperty("build-id", null);

            return new SimpleSdkInfo(name, version, buildId);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to read faststats.properties from META-INF", e);
        }
    }

    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    protected abstract boolean preSubmissionStart();

    @Contract(pure = true)
    public abstract String getProjectName();

    @Override
    @Contract(pure = true)
    public final Config getConfig() {
        return config;
    }

    @Override
    @Contract(pure = true)
    public final @Token String getToken() {
        return token;
    }

    @Override
    @Contract(pure = true)
    public final Optional<Metrics> metrics() {
        return Optional.ofNullable(metrics);
    }

    @Override
    @Contract(pure = true)
    public final Optional<FeatureFlagService> featureFlagService() {
        return Optional.ofNullable(featureFlagService);
    }

    @Contract(value = " -> new", pure = true)
    protected abstract Metrics.Factory metricsFactory();

    @Contract(value = " -> new", pure = true)
    protected FeatureFlagService.Factory featureFlagServiceFactory() {
        return new SimpleFeatureFlagService.Factory(this);
    }

    @Override
    @Contract(pure = true)
    public final Optional<ErrorTrackerService> errorTrackerService() {
        return Optional.ofNullable(errorTrackerService);
    }

    @Override
    public void ready() {
        if (ready) {
            logger.warn("%s#ready() was called twice; ignoring.", getClass().getSimpleName());
            return;
        }
        this.ready = true;
        if (errorTrackerService != null) errorTrackerService.startErrorSubmission();
        if (metrics instanceof final SimpleMetrics simpleMetrics) simpleMetrics.startSubmitting();
    }

    @Async.Schedule
    protected abstract void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit);

    @Override
    public void shutdown() {
        if (!ready) return;
        if (errorTrackerService != null) errorTrackerService.shutdown();
        if (featureFlagService instanceof final SimpleFeatureFlagService service) service.shutdown();
        if (metrics instanceof final SimpleMetrics simpleMetrics) simpleMetrics.shutdown();
        this.submissionActive = false;
        ready = false;
    }

    @Override
    @Contract(pure = true)
    public SdkInfo getSdkInfo() {
        return sdkInfo;
    }

    /**
     * Factory for creating a configured FastStats context.
     * <p>
     * Platform implementations may extend this class with constructors that accept
     * platform-specific objects before creating the context.
     *
     * @param <C> the context type created by this factory
     * @param <F> the concrete factory type
     * @since 0.24.0
     */
    public abstract static class Factory<C extends SimpleContext, F extends Factory<C, F>> {
        private @Nullable Function<Metrics.Factory, Metrics> metrics = null;
        private @Nullable Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService;
        private @Nullable ErrorTracker errorTracker;
        private @Nullable String tokenResource;

        /**
         * Configures the global/internal error tracker for the context.
         *
         * @param errorTracker the global/internal error tracker
         * @return this factory
         * @since 0.24.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public F errorTrackerService(final ErrorTracker errorTracker) {
            this.errorTracker = errorTracker;
            return self();
        }

        /**
         * Configures and creates the single metrics instance for the context.
         *
         * @param metrics a function that receives a new metrics factory and returns the built metrics instance
         * @return this factory
         * @since 0.24.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public F metrics(final Function<Metrics.Factory, Metrics> metrics) {
            this.metrics = metrics;
            return self();
        }

        /**
         * Configures and creates the single feature flag service instance for the context.
         *
         * @param featureFlagService a function that receives a new service factory and returns the built service instance
         * @return this factory
         * @since 0.24.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public F featureFlagService(final Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService) {
            this.featureFlagService = featureFlagService;
            return self();
        }

        /**
         * Creates the configured context.
         *
         * @return the configured context
         * @since 0.24.0
         */
        @Contract(value = " -> new", mutates = "io")
        public abstract C create();

        /**
         * Reads a token that was injected at build time from the {@value #TOKEN_RESOURCE_DIRECTORY} directory.
         * <p>
         * The token is read from the resource {@code META-INF/faststats/<id>.token}
         * using the given class loader. Surrounding whitespace is ignored.
         * <p>
         * The resource is namespaced by {@code id} because some platforms load every project from a shared class loader.
         * <p>
         * If the resource is missing, empty, unreadable or does not contain a valid token
         * (for example an unresolved build placeholder such as <code>${faststats.token}</code>), {@code null} is returned.
         * Passing {@code null} to the context creates a disabled context, so builds without a token
         * (such as forks) keep working without submitting any data.
         *
         * @param loader the class loader used to find the resource, usually the project's class loader
         * @param id     the project identifier, such as the plugin name or mod id
         * @return the token, or {@code null} if no token was injected
         * @since 0.32.0
         */
        @Contract(mutates = "io")
        protected static @Nullable String readToken(final ClassLoader loader, final String id) {
            final var path = TOKEN_RESOURCE_DIRECTORY + id + ".token";
            try (final var stream = loader.getResourceAsStream(path)) {
                if (stream == null) return null;
                final var token = new String(stream.readAllBytes(), UTF_8).strip();
                return token.matches(Token.PATTERN) ? token : null;
            } catch (final IOException e) {
                return null;
            }
        }

        /**
         * Returns the given token, or reads the token injected at build time if it is {@code null}.
         * <p>
         * The resource path is remembered, so a disabled context can name the expected file.
         *
         * @param token  the token passed to the factory, or {@code null}
         * @param loader the class loader used to find the resource, usually the project's class loader
         * @param id     the project identifier, such as the plugin name or mod id
         * @return the token, or {@code null} if none was passed or injected
         * @see #readToken(ClassLoader, String)
         * @since 0.32.1
         */
        @Contract(value = "!null, _, _ -> param1", mutates = "this,io")
        protected final @Nullable String resolveToken(@Token final @Nullable String token, final ClassLoader loader, final String id) {
            if (token != null) return token;
            this.tokenResource = TOKEN_RESOURCE_DIRECTORY + id + ".token";
            return readToken(loader, id);
        }

        /**
         * The resource directory build-time injected tokens are read from.
         *
         * @since 0.32.0
         */
        public static final String TOKEN_RESOURCE_DIRECTORY = "META-INF/faststats/";

        @SuppressWarnings("unchecked")
        private F self() {
            return (F) this;
        }
    }
}
