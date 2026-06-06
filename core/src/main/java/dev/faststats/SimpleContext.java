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

public non-sealed abstract class SimpleContext implements FastStatsContext {
    private final Logger logger = LoggerFactory.factory().getLogger(getClass());

    private final @Token String token;
    private final Config config;
    private final SdkInfo sdkInfo;

    protected volatile boolean ready = false;

    private @Nullable Metrics metrics;
    private @Nullable FeatureFlagService featureFlagService;
    private @Nullable SimpleErrorTrackerService errorTrackerService;
    private @Nullable PerformanceAnalyzer performanceAnalyzer;

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
    protected SimpleContext(final Factory<?, ?> factory, final Config config, final String name, @Token final String token) throws IllegalArgumentException {
        if (!token.matches(Token.PATTERN))
            throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");

        logger.setFilter(level -> config.debug());
        this.sdkInfo = constructSdkInfo(name);
        this.config = config;
        this.token = token;
    }

    @MustBeInvokedByOverriders
    protected final void initializeServices(final Factory<?, ?> factory) throws IllegalStateException {
        if (factory.metrics == null && factory.errorTracker == null && factory.featureFlagService == null)
            throw new IllegalStateException("Context created without any service attached, was this intentional?");

        if (!preSubmissionStart()) return;

        this.metrics = config.submitMetrics() && factory.metrics != null ? factory.metrics.apply(metricsFactory()) : null;
        this.errorTrackerService = config.errorTracking() && factory.errorTracker != null ? new SimpleErrorTrackerService(this, factory.errorTracker) : null;
        this.featureFlagService = factory.featureFlagService != null ? factory.featureFlagService.apply(new SimpleFeatureFlagService.Factory(this)) : null;
        this.performanceAnalyzer = config.submitMetrics() && factory.performanceAnalyzer != null
                ? factory.performanceAnalyzer.apply(new SimplePerformanceAnalyzer.Factory(this))
                : null;

        final var features = new HashSet<String>(4);
        features.add("metrics=" + (metrics != null ? "yes" : "no"));
        features.add("error-tracking=" + (errorTrackerService != null ? "yes" : "no"));
        features.add("feature-flags=" + (featureFlagService != null ? "yes" : "no"));
        features.add("performance=" + (performanceAnalyzer != null ? "yes" : "no"));

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
    @Contract(pure = true)
    public final Optional<PerformanceAnalyzer> performanceAnalyzer() {
        return Optional.ofNullable(performanceAnalyzer);
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
        if (performanceAnalyzer != null) performanceAnalyzer.shutdown();
        if (metrics instanceof final SimpleMetrics simpleMetrics) simpleMetrics.shutdown();
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
        private @Nullable Function<PerformanceAnalyzer.Factory, PerformanceAnalyzer> performanceAnalyzer;

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
         * Configures and creates the performance analyzer for the context.
         *
         * @param performanceAnalyzer a function that receives a new analyzer factory and returns the built analyzer
         * @return this factory
         * @since 0.26.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public F performanceAnalyzer(final Function<PerformanceAnalyzer.Factory, PerformanceAnalyzer> performanceAnalyzer) {
            this.performanceAnalyzer = performanceAnalyzer;
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
