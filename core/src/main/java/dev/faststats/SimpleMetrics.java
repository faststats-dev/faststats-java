package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.data.Metric;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApiStatus.Internal
// todo: revise
public abstract class SimpleMetrics extends SubmissionService implements Metrics {
    private static final String COLLECT_PATH = "/v1/collect";

    private final @Nullable Runnable flush;
    private final Set<Metric<?>> metrics;
    private final URI url = getServerUrl(
            "faststats.metrics-server",
            "https://metrics.faststats.dev"
    ).resolve(COLLECT_PATH);

    @Contract(mutates = "io")
    protected SimpleMetrics(final Factory factory) {
        super(factory.context);
        this.metrics = Set.copyOf(factory.metrics);
        this.flush = factory.flush;
    }

    @Async.Schedule
    void startSubmitting() {
        logger.info("Starting metrics submission task");
        final var initialDelay = TimeUnit.SECONDS.toMillis(Long.getLong("faststats.initial-delay", 30));
        final var period = TimeUnit.MINUTES.toMillis(30);
        context.scheduleAtFixedRate(this::submit, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public boolean isClientApplication() {
        return false;
    }

    private boolean submit() {
        if (!context.submissionsActive() || !context.getConfig().submitMetrics()) return false;
        try {
            if (submit(url, createData(), "metrics")) {
                if (flush != null) flush.run();
                return true;
            }
            return false;
        } catch (final Throwable t) {
            logger.error("Failed to submit metrics", t);
            return false;
        }
    }

    @Override
    protected String serverType() {
        return "metrics";
    }

    private static final String javaVendor = System.getProperty("java.vendor");
    private static final String javaVersion = System.getProperty("java.version");
    private static final String osArch = System.getProperty("os.arch");
    private static final String osName = System.getProperty("os.name");
    private static final String osVersion = System.getProperty("os.version");
    private static final int coreCount = Runtime.getRuntime().availableProcessors();

    private void appendInternalData(final JsonObject metrics) {
        metrics.addProperty("client", isClientApplication());
        metrics.addProperty("core_count", coreCount);
        metrics.addProperty("java_vendor", javaVendor);
        metrics.addProperty("java_version", javaVersion);
        metrics.addProperty("os_arch", osArch);
        metrics.addProperty("os_name", osName);
        metrics.addProperty("os_version", osVersion);
    }

    private void appendCustomData(final JsonObject metrics) {
        if (!context.getConfig().additionalMetrics()) return;
        this.metrics.forEach(metric -> {
            try {
                if (metrics.has(metric.getId())) {
                    logger.warn("Skipped duplicated metrics entry: %s", metric.getId());
                } else metric.getData().ifPresent(element -> {
                    if (!element.isJsonNull()) metrics.add(metric.getId(), element);
                    else logger.warn("Ignored illegal null entry in metric: %s", metric.getId());
                });
            } catch (final Throwable t) {
                logger.error("Failed to append custom metric data: %s", t, metric.getId());
                context.errorTrackerService().ifPresent(service -> service.globalErrorTracker().trackError(t));
            }
        });
    }

    public final void appendData(final JsonObject metrics) {
        appendInternalData(metrics);
        appendDefaultData(metrics);
        appendCustomData(metrics);
    }

    private JsonObject createData() {
        final var data = new JsonObject();
        final var metrics = new JsonObject();

        appendData(metrics);

        data.addProperty("project_name", context.getProjectName());
        data.addProperty("identifier", context.getConfig().serverId().toString());
        data.add("data", metrics);

        return data;
    }

    @Contract(mutates = "param1")
    protected abstract void appendDefaultData(JsonObject metrics);

    Set<String> metricIds() {
        return metrics.stream().map(Metric::getId).collect(Collectors.toUnmodifiableSet());
    }

    protected void shutdown() {
        try {
            logger.info("Shutting down metrics submission");
            submit();
        } catch (final Throwable t) {
            logger.error("Failed to submit metrics on shutdown", t);
        }
    }

    public abstract static class Factory implements Metrics.Factory {
        private @Nullable Runnable flush;
        private final Set<Metric<?>> metrics = new HashSet<>(0);
        protected final SimpleContext context;

        protected Factory(final SimpleContext context) {
            this.context = context;
        }

        @Override
        public Factory addMetric(final Metric<?> metric) throws IllegalArgumentException {
            if (!metrics.add(metric)) throw new IllegalArgumentException("Metric already added: " + metric.getId());
            return this;
        }

        @Override
        public Factory onFlush(final Runnable flush) {
            final var runnable = this.flush;
            if (runnable == null) {
                this.flush = flush;
            } else this.flush = () -> {
                runnable.run();
                flush.run();
            };
            return this;
        }
    }
}
