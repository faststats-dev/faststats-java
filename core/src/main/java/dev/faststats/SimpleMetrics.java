package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.data.Metric;
import dev.faststats.internal.Constants;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public abstract class SimpleMetrics implements Metrics {
    protected final Logger logger = LoggerFactory.factory().getLogger(getClass());

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private @Nullable ScheduledExecutorService executor = null;

    private final @Nullable ErrorTracker tracker;
    private final @Nullable Runnable flush;
    private final Set<Metric<?>> metrics;
    private final URI url;

    protected final FastStatsContext context;

    @Contract(mutates = "io")
    protected SimpleMetrics(final Factory factory) throws IllegalStateException {
        this(factory, getMetricsServerUrl());
    }

    private static URI getMetricsServerUrl() {
        final var property = System.getProperty("faststats.metrics-server");
        if (property != null) try {
            return new URI(property);
        } catch (final URISyntaxException e) {
            final var logger = LoggerFactory.factory().getLogger(SimpleMetrics.class);
            logger.error("Failed to parse metrics server url: %s", e, property);
        }
        return URI.create("https://metrics.faststats.dev/v1/collect");
    }

    @VisibleForTesting
    protected SimpleMetrics(
            final Factory factory,
            final URI url
    ) {
        this.context = factory.context;
        this.metrics = context.getConfig().additionalMetrics() ? Set.copyOf(factory.metrics) : Set.of();
        final var debug = context.getConfig().debug() || Boolean.getBoolean("faststats.debug");
        this.logger.setFilter(level -> debug || level.equals(Level.CONFIG));
        this.tracker = context.getConfig().errorTracking() ? factory.tracker : null;
        this.flush = factory.flush;
        this.url = url;
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

    protected abstract boolean preSubmissionStart();

    private void startSubmitting(final long initialDelay, final long period, final TimeUnit unit) {
        if (!preSubmissionStart()) return;

        final var enabled = Boolean.parseBoolean(System.getProperty("faststats.enabled", "true"));

        if (!context.getConfig().enabled() || !enabled) {
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

    @VisibleForTesting
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
                    .header("Authorization", "Bearer " + context.getToken())
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

        data.addProperty("identifier", context.getConfig().serverId().toString());
        data.add("data", metrics);

        getErrorTracker().map(SimpleErrorTracker.class::cast)
                .map(SimpleErrorTracker::getData)
                .filter(errors -> !errors.isEmpty())
                .ifPresent(errors -> data.add("errors", errors));
        return data;
    }

    @Override
    public Optional<ErrorTracker> getErrorTracker() {
        return Optional.ofNullable(tracker);
    }

    @Contract(mutates = "param1")
    protected abstract void appendDefaultData(JsonObject metrics);

    @Override
    public void shutdown() {
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

    public abstract static class Factory implements Metrics.Factory {
        private final Set<Metric<?>> metrics = new HashSet<>(0);
        protected final FastStatsContext context;
        private @Nullable ErrorTracker tracker;
        private @Nullable Runnable flush;

        protected Factory(final FastStatsContext context) {
            this.context = context;
        }

        @Override
        public Factory addMetric(final Metric<?> metric) throws IllegalArgumentException {
            if (!metrics.add(metric)) throw new IllegalArgumentException("Metric already added: " + metric.getId());
            return this;
        }

        @Override
        public Factory onFlush(final Runnable flush) {
            this.flush = flush;
            return this;
        }

        @Override
        public Factory errorTracker(final ErrorTracker tracker) {
            this.tracker = tracker;
            return this;
        }
    }
}
