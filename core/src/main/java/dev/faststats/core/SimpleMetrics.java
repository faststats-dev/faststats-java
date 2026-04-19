package dev.faststats.core;

import com.google.gson.JsonObject;
import dev.faststats.core.data.Metric;
import dev.faststats.core.flags.FeatureFlagService;
import dev.faststats.core.internal.Constants;
import dev.faststats.core.internal.Logger;
import dev.faststats.core.internal.LoggerFactory;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class SimpleMetrics implements Metrics {
    protected final Logger logger = LoggerFactory.factory().getLogger(getClass().getName());
    private static final URI defaultUrl = URI.create("https://metrics.faststats.dev/v1/collect");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private @Nullable ScheduledExecutorService executor = null;

    private final URI url;
    private final Set<Metric<?>> metrics;
    private final Config config;
    private final @Token String token;
    private final @Nullable ErrorTracker tracker;
    private final @Nullable Runnable flush;
    private final @Nullable FeatureFlagService flagService;

    @Contract(mutates = "io")
    @SuppressWarnings("PatternValidation")
    protected SimpleMetrics(final Factory<?, ?> factory, final Config config) throws IllegalStateException {
        if (factory.token == null) throw new IllegalStateException("Token must be specified");

        this.config = config;
        this.token = factory.token;
        this.metrics = config.additionalMetrics ? Set.copyOf(factory.metrics) : Set.of();
        final var debug = config.debug() || Boolean.getBoolean("faststats.debug");
        this.logger.setFilter(record -> debug || record.getLevel().equals(Level.CONFIG));
        this.tracker = config.errorTracking ? factory.tracker : null;
        this.flush = factory.flush;
        this.flagService = factory.flagService;
        this.url = getMetricsServerUrl();
    }

    private URI getMetricsServerUrl() {
        final var property = System.getProperty("faststats.metrics-server");
        try {
            return property != null ? new URI(property) : defaultUrl;
        } catch (final URISyntaxException e) {
            logger.error("Failed to parse metrics server url: %s", e, property);
            return defaultUrl;
        }
    }

    @Contract(mutates = "io")
    protected SimpleMetrics(final Factory<?, ?> factory, final Path config) throws IllegalStateException {
        this(factory, Config.read(config));
    }

    @VisibleForTesting
    protected SimpleMetrics(
            final Config config,
            final Set<Metric<?>> metrics,
            @Token final String token,
            @Nullable final ErrorTracker tracker,
            @Nullable final Runnable flush,
            final URI url,
            final boolean debug
    ) {
        this.metrics = config.additionalMetrics ? Set.copyOf(metrics) : Set.of();
        this.config = config;
        this.logger.setLevel(debug ? Level.ALL : Level.OFF);
        this.token = token;
        this.tracker = tracker;
        this.flush = flush;
        this.url = url;
        this.flagService = null;
    }

    protected String getOnboardingMessage() {
        return """
                This piece of software uses FastStats to collect anonymous usage statistics.
                No personal or identifying information is ever collected.
                To opt out, set 'enabled=false' in the metrics configuration file.
                Learn more at: https://faststats.dev/info
                
                Since this is your first start with FastStats, metrics submission will not start
                until you restart the server to allow you to opt out if you prefer.""";
    }

    protected long getInitialDelay() {
        return TimeUnit.SECONDS.toMillis(Long.getLong("faststats.initial-delay", 30));
    }

    protected long getPeriod() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Async.Schedule
    @MustBeInvokedByOverriders
    protected void startSubmitting() {
        startSubmitting(getInitialDelay(), getPeriod(), TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("PatternValidation")
    private void startSubmitting(final long initialDelay, final long period, final TimeUnit unit) {
        if (Boolean.getBoolean("faststats.first-run")) {
            logger.info("Skipping metrics submission due to first-run flag");
            return;
        }

        if (config.firstRun) {

            var separatorLength = 0;
            final var split = getOnboardingMessage().split("\n");
            for (final var s : split) if (s.length() > separatorLength) separatorLength = s.length();

            logger.info("-".repeat(separatorLength));
            for (final var s : split) logger.info(s);
            logger.info("-".repeat(separatorLength));

            System.setProperty("faststats.first-run", "true");
            if (!config.externallyManaged()) return;
        }

        final var enabled = Boolean.parseBoolean(System.getProperty("faststats.enabled", "true"));

        if (!config.enabled() || !enabled) {
            logger.warn("Metrics disabled, not starting submission");
            return;
        }

        if (isSubmitting()) {
            logger.warn("Metrics already submitting, not starting again");
            return;
        }

        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> { // todo: SINGLE THREAD??? what was i smoking?
            final var thread = new Thread(runnable, "metrics-submitter");
            thread.setDaemon(true);
            return thread;
        });

        logger.info("Starting metrics submission");
        executor.scheduleAtFixedRate(this::submit, Math.max(0, initialDelay), Math.max(1000, period), unit);
    }

    protected boolean isSubmitting() {
        return executor != null && !executor.isShutdown();
    }

    public boolean submit() {
        try {
            return submitNow();
        } catch (final Throwable t) {
            logger.error("Failed to submit metrics", t);
            return false;
        }
    }

    private boolean submitNow() throws IOException {
        final var data = createData().toString();
        final var bytes = data.getBytes(UTF_8);

        logger.info("Uncompressed data: %s", data);

        try (final var byteOutput = new ByteArrayOutputStream();
             final var output = new GZIPOutputStream(byteOutput)) {

            output.write(bytes);
            output.finish();

            final var compressed = byteOutput.toByteArray();
            logger.info("Compressed size: %s bytes", compressed.length);

            final var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
                    .header("Content-Encoding", "gzip")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "FastStats Metrics " + Constants.SDK_NAME + "/" + Constants.SDK_VERSION)
                    .timeout(Duration.ofSeconds(3))
                    .uri(url)
                    .build();

            logger.info("Sending metrics to: %s", url);
            try {
                final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
                final var statusCode = response.statusCode();
                final var body = response.body();

                if (statusCode >= 200 && statusCode < 300) {
                    logger.info("Metrics submitted with status code: %s (%s)", statusCode, body);
                    getErrorTracker().map(SimpleErrorTracker.class::cast).ifPresent(SimpleErrorTracker::clear);
                    if (flush != null) flush.run();
                    return true;
                } else if (statusCode >= 300 && statusCode < 400) {
                    logger.warn("Received redirect response from metrics server: %s (%s)", statusCode, body);
                } else if (statusCode >= 400 && statusCode < 500) {
                    logger.error("Submitted invalid request to metrics server: %s (%s)", null, statusCode, body);
                } else if (statusCode >= 500 && statusCode < 600) {
                    logger.error("Received server error response from metrics server: %s (%s)", null, statusCode, body);
                } else {
                    logger.warn("Received unexpected response from metrics server: %s (%s)", statusCode, body);
                }
            } catch (final HttpConnectTimeoutException t) {
                logger.error("Metrics submission timed out after 3 seconds: %s", null, url);
            } catch (final ConnectException t) {
                logger.error("Failed to connect to metrics server: %s", null, url);
            } catch (final Throwable t) {
                logger.error("Failed to submit metrics", t);
            }
            return false;
        }
    }

    private static final String javaVendor = System.getProperty("java.vendor");
    private static final String javaVersion = System.getProperty("java.version");
    private static final String osArch = System.getProperty("os.arch");
    private static final String osName = System.getProperty("os.name");
    private static final String osVersion = System.getProperty("os.version");
    private static final int coreCount = Runtime.getRuntime().availableProcessors();

    protected JsonObject createData() {
        final var data = new JsonObject();
        final var metrics = new JsonObject();

        metrics.addProperty("core_count", coreCount);
        metrics.addProperty("java_vendor", javaVendor);
        metrics.addProperty("java_version", javaVersion);
        metrics.addProperty("os_arch", osArch);
        metrics.addProperty("os_name", osName);
        metrics.addProperty("os_version", osVersion);

        try {
            appendDefaultData(metrics);
        } catch (final Throwable t) {
            logger.error("Failed to append default data", t);
            getErrorTracker().ifPresent(tracker -> tracker.trackError(t));
        }

        this.metrics.forEach(metric -> {
            try {
                metric.getData().ifPresent(element -> metrics.add(metric.getId(), element));
            } catch (final Throwable t) {
                logger.error("Failed to build metric data: %s", t, metric.getId());
                getErrorTracker().ifPresent(tracker -> tracker.trackError(t));
            }
        });

        data.addProperty("identifier", config.serverId().toString());
        data.add("data", metrics);

        getErrorTracker().map(SimpleErrorTracker.class::cast)
                .map(tracker -> tracker.getData(Constants.BUILD_ID))
                .filter(errors -> !errors.isEmpty())
                .ifPresent(errors -> data.add("errors", errors));
        return data;
    }

    @Override
    public @Token String getToken() {
        return token;
    }

    @Override
    public Optional<ErrorTracker> getErrorTracker() {
        return Optional.ofNullable(tracker);
    }

    @Override
    public Optional<FeatureFlagService> getFeatureFlagService() {
        return Optional.ofNullable(flagService);
    }

    @Override
    public Metrics.Config getConfig() {
        return config;
    }

    @Contract(mutates = "param1")
    protected abstract void appendDefaultData(JsonObject metrics);

    @Override
    public void shutdown() {
        if (flagService != null) flagService.shutdown();
        getErrorTracker().ifPresent(ErrorTracker::detachErrorContext);
        if (executor != null) try {
            logger.info("Shutting down metrics submission");
            executor.shutdown();
            getErrorTracker().map(SimpleErrorTracker.class::cast)
                    .filter(SimpleErrorTracker::needsFlushing)
                    .ifPresent(ignored -> submit());
        } catch (final Throwable t) {
            logger.error("Failed to submit metrics on shutdown", t);
        } finally {
            executor = null;
        }
    }

    public abstract static class Factory<T, F extends Metrics.Factory<T, F>> implements Metrics.Factory<T, F> {
        private final Set<Metric<?>> metrics = new HashSet<>(0);
        private @Nullable ErrorTracker tracker;
        private @Nullable FeatureFlagService flagService;
        private @Nullable Runnable flush;
        private @Nullable String token;

        @Override
        @SuppressWarnings("unchecked")
        public F addMetric(final Metric<?> metric) throws IllegalArgumentException {
            if (!metrics.add(metric)) throw new IllegalArgumentException("Metric already added: " + metric.getId());
            return (F) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public F onFlush(final Runnable flush) {
            this.flush = flush;
            return (F) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public F errorTracker(final ErrorTracker tracker) {
            this.tracker = tracker;
            return (F) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public F featureFlagService(final FeatureFlagService service) {
            this.flagService = service;
            return (F) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public F token(@Token final String token) throws IllegalArgumentException {
            if (!token.matches(Token.PATTERN)) {
                throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");
            }
            this.token = token;
            return (F) this;
        }
    }

    public record Config(
            UUID serverId,
            boolean additionalMetrics,
            boolean debug,
            boolean enabled,
            boolean errorTracking,
            boolean firstRun,
            boolean externallyManaged
    ) implements Metrics.Config {

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
        public static Config read(final Path file) throws RuntimeException {
            return read(file, DEFAULT_COMMENT, false, false);
        }

        @Contract(mutates = "io")
        public static Config read(final Path file, final String comment, final boolean externallyManaged, final boolean externallyEnabled) throws RuntimeException {
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

            return new Config(serverId, additionalMetrics, debug, enabled, errorTracking, firstRun, externallyManaged);
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
}
