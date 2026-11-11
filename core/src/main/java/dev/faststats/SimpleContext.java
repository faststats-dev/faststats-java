package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The FastStats context implementation shared by all platforms.
 * <p>
 * Platforms do not extend this class. Instead, they configure a {@link Builder}
 * with their platform-specific pieces, such as the logger, configuration, scheduler
 * and default metrics data, and expose their own factory on top of it.
 *
 * @since 0.24.0
 */
public final class SimpleContext implements FastStatsContext {
    /**
     * The resource directory build-time injected tokens are read from.
     *
     * @since 0.32.0
     */
    public static final String TOKEN_RESOURCE_DIRECTORY = "META-INF/faststats/";

    private final @Token String token;
    private final boolean enabled;
    private volatile Config config;
    private final SdkInfo sdkInfo;
    private final String projectName;
    private final boolean clientApplication;

    private final LoggerFactory loggerFactory;
    private final Logger logger;

    private final @Nullable Scheduler scheduler;
    private final Set<Runnable> cancellations = new CopyOnWriteArraySet<>();
    private @Nullable ScheduledExecutorService executor;

    private final @Nullable BiConsumer<SimpleContext, JsonObject> defaultMetrics;
    private final List<Consumer<FastStatsContext>> readyHooks;
    private final List<Consumer<FastStatsContext>> shutdownHooks;

    private volatile boolean ready = false;
    private volatile boolean submitting = false;

    private final @Nullable SimpleMetrics metrics;
    private final @Nullable SimpleFeatureFlagService featureFlagService;
    private final @Nullable SimpleErrorTrackerService errorTrackerService;

    private SimpleContext(final Builder builder, @Token final @Nullable String token) {
        this.loggerFactory = require(builder.loggerFactory, "loggerFactory");
        this.config = require(builder.config, "config");
        this.projectName = require(builder.projectName, "projectName");
        loggerFactory.setDebug(config.debug());
        this.logger = loggerFactory.getLogger(SimpleContext.class);

        this.sdkInfo = constructSdkInfo(builder.sdkName);
        this.token = token != null ? token : "";
        this.enabled = token != null;
        this.clientApplication = builder.clientApplication;
        this.scheduler = builder.scheduler;
        this.defaultMetrics = builder.defaultMetrics;
        this.readyHooks = List.copyOf(builder.readyHooks);
        this.shutdownHooks = List.copyOf(builder.shutdownHooks);

        if (!enabled) {
            logger.warn("No valid FastStats token provided for %s, FastStats is disabled. Pass a token to the factory or inject it at build time into %s",
                    projectName, TOKEN_RESOURCE_DIRECTORY + (builder.tokenId != null ? builder.tokenId : "<id>") + ".token");
        }

        this.metrics = enabled && builder.metrics != null
                ? (SimpleMetrics) builder.metrics.apply(new SimpleMetrics.Factory(this))
                : null;
        this.errorTrackerService = enabled && builder.errorTracker != null
                ? new SimpleErrorTrackerService(this, builder.errorTracker)
                : null;
        this.featureFlagService = enabled && builder.featureFlagService != null
                ? (SimpleFeatureFlagService) builder.featureFlagService.apply(new SimpleFeatureFlagService.Factory(this))
                : null;

        if (enabled) {
            logger.info("Created FastStats context for %s using %s (metrics=%s, error-tracking=%s, feature-flags=%s)",
                    projectName, sdkInfo.getUserAgent(),
                    metrics != null ? "yes" : "no",
                    errorTrackerService != null ? "yes" : "no",
                    featureFlagService != null ? "yes" : "no"
            );
        }
    }

    private static <T> T require(final @Nullable T value, final String name) throws IllegalStateException {
        if (value == null) throw new IllegalStateException("Missing required context property: " + name);
        return value;
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
    public boolean isEnabled() {
        return enabled;
    }

    private static SdkInfo constructSdkInfo(final String name) throws UncheckedIOException, IllegalStateException, IllegalArgumentException {
        try (final var stream = SimpleContext.class.getResourceAsStream("/META-INF/faststats.properties")) {
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

    boolean isClientApplication() {
        return clientApplication;
    }

    @ApiStatus.Internal
    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @ApiStatus.Internal
    public String getProjectName() {
        return projectName;
    }

    @Override
    @Contract(pure = true)
    public Config getConfig() {
        return config;
    }

    @Override
    @Contract(pure = true)
    public @Token String getToken() {
        return token;
    }

    @Override
    @Contract(pure = true)
    public Optional<Metrics> metrics() {
        return Optional.ofNullable(metrics);
    }

    @Override
    @Contract(pure = true)
    public Optional<FeatureFlagService> featureFlagService() {
        return Optional.ofNullable(featureFlagService);
    }

    @Override
    @Contract(pure = true)
    public Optional<ErrorTrackerService> errorTrackerService() {
        return Optional.ofNullable(errorTrackerService);
    }

    boolean isSubmitting() {
        return submitting;
    }

    /**
     * Starts submitting data to FastStats.
     * <p>
     * Platforms call this once submission is allowed, for example when the context is
     * {@linkplain #ready() ready} and the first run onboarding or consent screen is done.
     * Does nothing if the context is disabled or submission is already active.
     * <p>
     * If a submission task cannot be scheduled, every task scheduled so far is canceled,
     * submission stays inactive and the failure is rethrown, so it can be started again later.
     *
     * @throws RuntimeException if the platform scheduler rejected a submission task
     * @since 0.33.0
     */
    @ApiStatus.Internal
    public synchronized void startSubmission() {
        if (!enabled || submitting) return;
        try {
            if (errorTrackerService != null) errorTrackerService.startErrorSubmission();
            if (metrics != null) metrics.startSubmitting();
            this.submitting = true;
        } catch (final Throwable t) {
            cancelScheduledTasks();
            throw t;
        }
    }

    /**
     * Stops submitting data to FastStats without flushing pending data.
     * <p>
     * Submission can be started again with {@link #startSubmission()}.
     *
     * @since 0.33.0
     */
    @ApiStatus.Internal
    public synchronized void stopSubmission() {
        if (!submitting) return;
        this.submitting = false;
        cancelScheduledTasks();
    }

    private void cancelScheduledTasks() {
        cancellations.forEach(cancellation -> {
            try {
                cancellation.run();
            } catch (final Throwable t) {
                logger.error("Failed to cancel scheduled task", t);
            }
        });
        cancellations.clear();
    }

    /**
     * Replaces the configuration of this context, for example after the user changed their consent.
     * <p>
     * Services read the configuration on every submission, so the change applies to the next one.
     * Submission is not started or stopped by this method, see {@link #startSubmission()} and {@link #stopSubmission()}.
     *
     * @param config the new configuration
     * @throws IllegalArgumentException if the server id differs from the current configuration
     * @since 0.33.0
     */
    @ApiStatus.Internal
    @Contract(mutates = "this")
    public void updateConfig(final Config config) throws IllegalArgumentException {
        if (!config.serverId().equals(this.config.serverId()))
            throw new IllegalArgumentException("The server id of a context cannot change");
        this.config = config;
        loggerFactory.setDebug(config.debug());
    }

    void appendDefaultMetrics(final JsonObject metrics) {
        try {
            if (defaultMetrics != null) defaultMetrics.accept(this, metrics);
        } catch (final Throwable t) {
            logger.error("Failed to append default metric data", t);
            errorTrackerService().ifPresent(service -> service.globalErrorTracker().trackError(t));
        }
    }

    @Override
    public void ready() {
        if (ready) {
            logger.warn("FastStats context for %s was made ready twice; ignoring.", projectName);
            return;
        }
        for (final var hook : readyHooks) {
            try {
                hook.accept(this);
            } catch (final Throwable t) {
                logger.print(Logger.LogLevel.ERROR, t, "Failed to make the FastStats context for %s ready: %s"
                        .formatted(projectName, t.getMessage()));
                return;
            }
        }
        this.ready = true;
    }

    @Async.Schedule
    void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        if (scheduler != null) {
            cancellations.add(scheduler.scheduleAtFixedRate(task, initialDelay, period, unit));
            return;
        }
        synchronized (this) {
            if (executor == null) executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                final var thread = new Thread(runnable, "faststats-submitter");
                thread.setDaemon(true);
                return thread;
            });
            final var future = executor.scheduleAtFixedRate(task, initialDelay, period, unit);
            cancellations.add(() -> future.cancel(true));
        }
    }

    @Override
    public void shutdown() {
        // flushes pending data if submission is active
        if (errorTrackerService != null) errorTrackerService.shutdown();
        if (featureFlagService != null) featureFlagService.shutdown();
        if (metrics != null) metrics.shutdown();
        stopSubmission();
        this.ready = false;

        synchronized (this) {
            if (executor != null) executor.shutdown();
            executor = null;
        }

        for (final var hook : shutdownHooks) {
            try {
                hook.accept(this);
            } catch (final Throwable t) {
                logger.error("Failed to run shutdown hook", t);
            }
        }
    }

    @Override
    @Contract(pure = true)
    public SdkInfo getSdkInfo() {
        return sdkInfo;
    }

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
    public static @Nullable String readToken(final ClassLoader loader, final String id) {
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
     * Schedules repeating tasks on the platform scheduler.
     *
     * @since 0.33.0
     */
    @FunctionalInterface
    public interface Scheduler {
        /**
         * Schedules a task to run asynchronously at a fixed rate.
         *
         * @param task         the task to run
         * @param initialDelay the delay before the first run
         * @param period       the period between runs
         * @param unit         the time unit of the delay and period
         * @return an action that cancels the scheduled task
         * @since 0.33.0
         */
        Runnable scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit);
    }

    /**
     * Builder for {@link SimpleContext} on platforms that can ask the user for consent.
     * <p>
     * Platforms read {@link #isConsentRequired()} from their {@linkplain #onBuild(BiConsumer) build hook},
     * as the user may require consent after the platform configured the builder.
     *
     * @since 0.33.0
     */
    public static final class ConsentBuilder extends Builder implements FastStatsContext.ConsentBuilder {
        private volatile boolean requireConsent = false;

        /**
         * Creates a new context builder.
         *
         * @param sdkName the name of the SDK, such as the platform name
         * @since 0.33.0
         */
        public ConsentBuilder(final String sdkName) {
            super(sdkName);
        }

        @Override
        @Contract(value = " -> this", mutates = "this")
        public ConsentBuilder requireConsent() {
            this.requireConsent = true;
            return this;
        }

        @Override
        @Contract(pure = true)
        public boolean isConsentRequired() {
            return requireConsent;
        }

        @Override
        public ConsentBuilder loggerFactory(final LoggerFactory loggerFactory) {
            return (ConsentBuilder) super.loggerFactory(loggerFactory);
        }

        @Override
        public ConsentBuilder config(final Config config) {
            return (ConsentBuilder) super.config(config);
        }

        @Override
        public ConsentBuilder projectName(final String projectName) {
            return (ConsentBuilder) super.projectName(projectName);
        }

        @Override
        public ConsentBuilder token(@Token final @Nullable String token) {
            return (ConsentBuilder) super.token(token);
        }

        @Override
        public ConsentBuilder tokenResource(final ClassLoader loader, final String id) {
            return (ConsentBuilder) super.tokenResource(loader, id);
        }

        @Override
        public ConsentBuilder scheduler(final Scheduler scheduler) {
            return (ConsentBuilder) super.scheduler(scheduler);
        }

        @Override
        public ConsentBuilder defaultMetrics(final BiConsumer<SimpleContext, JsonObject> defaultMetrics) {
            return (ConsentBuilder) super.defaultMetrics(defaultMetrics);
        }

        @Override
        public ConsentBuilder clientApplication(final boolean clientApplication) {
            return (ConsentBuilder) super.clientApplication(clientApplication);
        }

        @Override
        public ConsentBuilder onBuild(final BiConsumer<SimpleContext, Builder> hook) {
            return (ConsentBuilder) super.onBuild(hook);
        }

        @Override
        public ConsentBuilder onReady(final Consumer<FastStatsContext> hook) {
            return (ConsentBuilder) super.onReady(hook);
        }

        @Override
        public ConsentBuilder onShutdown(final Consumer<FastStatsContext> hook) {
            return (ConsentBuilder) super.onShutdown(hook);
        }

        @Override
        public ConsentBuilder errorTrackerService(final ErrorTracker errorTracker) {
            return (ConsentBuilder) super.errorTrackerService(errorTracker);
        }

        @Override
        public ConsentBuilder metrics(final Function<Metrics.Factory, Metrics> metrics) {
            return (ConsentBuilder) super.metrics(metrics);
        }

        @Override
        public ConsentBuilder featureFlagService(final Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService) {
            return (ConsentBuilder) super.featureFlagService(featureFlagService);
        }
    }

    /**
     * Builder for {@link SimpleContext}.
     * <p>
     * Platforms configure the platform-specific properties of the builder
     * and hand it to their users as a {@link FastStatsContext.Builder},
     * which only exposes the service configuration.
     *
     * @since 0.33.0
     */
    public static sealed class Builder implements FastStatsContext.Builder {
        private final String sdkName;

        private @Nullable LoggerFactory loggerFactory;
        private @Nullable Config config;
        private @Nullable String projectName;
        @Token
        private @Nullable String token;
        private @Nullable ClassLoader tokenLoader;
        private @Nullable String tokenId;
        private @Nullable Scheduler scheduler;
        private @Nullable BiConsumer<SimpleContext, JsonObject> defaultMetrics;
        private boolean clientApplication = false;
        private final List<Consumer<FastStatsContext>> readyHooks = new ArrayList<>(1);
        private final List<Consumer<FastStatsContext>> shutdownHooks = new ArrayList<>(1);
        private final List<Consumer<SimpleContext>> buildHooks = new ArrayList<>(1);

        private @Nullable Function<Metrics.Factory, Metrics> metrics;
        private @Nullable Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService;
        private @Nullable ErrorTracker errorTracker;

        /**
         * Creates a new context builder.
         *
         * @param sdkName the name of the SDK, such as the platform name
         * @since 0.33.0
         */
        public Builder(final String sdkName) {
            this.sdkName = sdkName;
        }

        /**
         * Sets the logger factory used by the context and its services.
         *
         * @param loggerFactory the logger factory
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder loggerFactory(final LoggerFactory loggerFactory) {
            this.loggerFactory = loggerFactory;
            return this;
        }

        /**
         * Sets the configuration shared by the context and its services.
         *
         * @param config the configuration
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder config(final Config config) {
            this.config = config;
            return this;
        }

        /**
         * Sets the name of the project submitting data, such as the plugin name or mod id.
         *
         * @param projectName the project name
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder projectName(final String projectName) {
            this.projectName = projectName;
            return this;
        }

        @Override
        @Contract(value = "_ -> this", mutates = "this")
        public Builder token(@Token final @Nullable String token) {
            this.token = token;
            return this;
        }

        /**
         * Configures where the token injected at build time is read from if no token was set.
         *
         * @param loader the class loader used to find the resource, usually the project's class loader
         * @param id     the project identifier, such as the plugin name or mod id
         * @return this builder
         * @see #readToken(ClassLoader, String)
         * @since 0.33.0
         */
        @Contract(value = "_, _ -> this", mutates = "this")
        public Builder tokenResource(final ClassLoader loader, final String id) {
            this.tokenLoader = loader;
            this.tokenId = id;
            return this;
        }

        /**
         * Sets the scheduler used to run submission tasks.
         * <p>
         * If no scheduler is set, a daemon thread owned by the context is used.
         *
         * @param scheduler the scheduler
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder scheduler(final Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * Sets the platform default data appended to every metrics submission,
         * such as the player count or platform version.
         *
         * @param defaultMetrics appends the default data to the given metrics object
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder defaultMetrics(final BiConsumer<SimpleContext, JsonObject> defaultMetrics) {
            this.defaultMetrics = defaultMetrics;
            return this;
        }

        /**
         * Marks the context as running inside a client application, such as a game client.
         *
         * @param clientApplication whether the context runs on a client
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder clientApplication(final boolean clientApplication) {
            this.clientApplication = clientApplication;
            return this;
        }

        /**
         * Adds a hook that runs right after the context was built, before {@link #build()} returns it.
         * <p>
         * Platforms use this to wire the context into their lifecycle,
         * for example to make it ready once the server started.
         * Failures are not caught and abort the build.
         * <p>
         * Unlike the other hooks, this hook is only available to platforms,
         * as it is not part of {@link FastStatsContext.Builder}.
         *
         * @param hook the hook, receiving the context and this builder
         * @return this builder
         * @since 0.33.0
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder onBuild(final BiConsumer<SimpleContext, Builder> hook) {
            this.buildHooks.add(context -> hook.accept(context, this));
            return this;
        }

        @Override
        @Contract(value = "_ -> this", mutates = "this")
        public Builder onReady(final Consumer<FastStatsContext> hook) {
            this.readyHooks.add(hook);
            return this;
        }

        @Override
        @Contract(value = "_ -> this", mutates = "this")
        public Builder onShutdown(final Consumer<FastStatsContext> hook) {
            this.shutdownHooks.add(hook);
            return this;
        }

        @Override
        @Contract(value = "_ -> this", mutates = "this")
        public Builder errorTrackerService(final ErrorTracker errorTracker) {
            this.errorTracker = errorTracker;
            return this;
        }

        @Override
        @Contract(value = "_ -> this", mutates = "this")
        public Builder metrics(final Function<Metrics.Factory, Metrics> metrics) {
            this.metrics = metrics;
            return this;
        }

        @Override
        @Contract(value = "_ -> this", mutates = "this")
        public Builder featureFlagService(final Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService) {
            this.featureFlagService = featureFlagService;
            return this;
        }

        @Override
        @Contract(value = " -> new", mutates = "io")
        @SuppressWarnings("PatternValidation")
        public SimpleContext build() throws IllegalArgumentException, IllegalStateException, UncheckedIOException {
            if (metrics == null && errorTracker == null && featureFlagService == null)
                throw new IllegalStateException("Context created without any service attached, was this intentional?");
            if (token != null && !token.matches(Token.PATTERN))
                throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");
            final var token = this.token != null ? this.token
                    : tokenLoader != null && tokenId != null ? readToken(tokenLoader, tokenId) : null;
            final var context = new SimpleContext(this, token);
            buildHooks.forEach(hook -> hook.accept(context));
            return context;
        }
    }
}
