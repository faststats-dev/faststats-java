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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

// todo: revise
public non-sealed abstract class SimpleContext implements FastStatsContext {
    private final @Token String token;
    private volatile Config config;
    private final SdkInfo sdkInfo;

    private final LoggerFactory loggerFactory;
    private final Logger logger;

    protected volatile boolean ready = false;
    private volatile boolean submissionActive;

    private @Nullable Metrics metrics;
    private @Nullable FeatureFlagService featureFlagService;
    private @Nullable SimpleErrorTrackerService errorTrackerService;

    /**
     * Creates a new context that stores the shared configuration and token for all FastStats services.
     *
     * @param config the shared configuration
     * @param name   the name of the SDK
     * @param token  the FastStats project token
     * @throws IllegalArgumentException if the token is invalid
     * @throws IllegalArgumentException if the SDK information is invalid
     * @throws IllegalStateException    if the SDK information is incomplete or missing
     * @throws UncheckedIOException     if an IO error occurs
     * @since 0.24.0
     */
    protected SimpleContext(final Factory<?, ?> factory, final LoggerFactory loggerFactory, final Config config, final String name, @Token final String token) throws IllegalArgumentException {
        if (!token.matches(Token.PATTERN))
            throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");

        this.loggerFactory = loggerFactory;
        loggerFactory.setDebug(config.debug());
        this.logger = loggerFactory.getLogger(getClass());

        this.sdkInfo = constructSdkInfo(name);
        this.config = config;
        this.token = token;
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
        return submissionActive && config.submitMetrics() ? Optional.ofNullable(metrics) : Optional.empty();
    }

    @Override
    @Contract(pure = true)
    public final Optional<FeatureFlagService> featureFlagService() {
        return submissionActive ? Optional.ofNullable(featureFlagService) : Optional.empty();
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
        return submissionActive && config.errorTracking() ? Optional.ofNullable(errorTrackerService) : Optional.empty();
    }

    @Override
    public synchronized void ready() {
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

    final FastStatsRegistration registration() {
        final var additionalMetrics = metrics instanceof final SimpleMetrics simpleMetrics
                ? simpleMetrics.metricIds()
                : Set.<String>of();
        return new FastStatsRegistration(
                getProjectName(), sdkInfo.getName(), sdkInfo.getVersion(),
                metrics != null, errorTrackerService != null, featureFlagService != null, additionalMetrics
        );
    }

    final synchronized void startSubmissions(final Config config) {
        final var wasActive = submissionActive;
        this.config = config;
        loggerFactory.setDebug(config.debug());
        if ((!wasActive || !config.errorTracking()) && errorTrackerService != null) errorTrackerService.clearPending();
        submissionActive = true;
    }

    final synchronized void stopSubmissions(final Config config) {
        this.config = config;
        loggerFactory.setDebug(config.debug());
        submissionActive = false;
        if (errorTrackerService != null) errorTrackerService.clearPending();
    }

    final boolean submissionsActive() {
        return submissionActive;
    }

    @Override
    public synchronized void shutdown() {
        if (!ready) return;
        if (submissionActive && config.errorTracking() && errorTrackerService != null) errorTrackerService.shutdown();
        else if (errorTrackerService != null) errorTrackerService.clearPending();
        if (featureFlagService instanceof final SimpleFeatureFlagService service) service.shutdown();
        if (submissionActive && config.submitMetrics() && metrics instanceof final SimpleMetrics simpleMetrics)
            simpleMetrics.shutdown();
        submissionActive = false;
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

        @SuppressWarnings("unchecked")
        private F self() {
            return (F) this;
        }
    }
}
