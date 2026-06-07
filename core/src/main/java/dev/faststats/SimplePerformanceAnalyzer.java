package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;
import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Simple performance analyzer implementation.
 *
 * @since 0.26.0
 */
final class SimplePerformanceAnalyzer extends SubmissionService implements PerformanceAnalyzer {
    private static final String PERFORMANCE_PATH = "/v1/performance";
    private static final String OVERFLOW_SHAPE = "__overflow__";
    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:''|[^'])*'|\"(?:\"\"|[^\"])*\"");
    private static final Pattern NUMBER_LITERAL = Pattern.compile("(?<![\\w.])-?\\d+(?:\\.\\d+)?(?![\\w.])");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern IN_LIST = Pattern.compile("(?i)\\bin\\s*\\((?:\\s*\\?\\s*,){2,}\\s*\\?\\s*\\)");

    private final URI url = getServerUrl(
            "faststats.performance-server",
            "https://metrics.faststats.dev"
    ).resolve(PERFORMANCE_PATH);

    private final SqlPerformanceOptions sqlOptions;
    private final JfrPerformanceOptions jfrOptions;
    private final List<Map.Entry<Pattern, String>> sqlAnonymizationEntries;

    private final Map<QueryKey, SqlStats> sqlStats = new ConcurrentHashMap<>();
    private final Map<String, JfrStats> jfrStats = new ConcurrentHashMap<>();
    private final Map<String, BlockStats> blockStats = new ConcurrentHashMap<>();

    private SimplePerformanceAnalyzer(final Factory factory) {
        super(factory.context);
        this.sqlOptions = factory.sqlOptions;
        this.jfrOptions = factory.jfrOptions;
        this.sqlAnonymizationEntries = sqlAnonymizationEntries(sqlOptions.anonymizationEntries());
    }

    @Override
    protected String serverType() {
        return "performance";
    }

    @Override
    public DataSource wrap(final DataSource dataSource) {
        return (DataSource) Proxy.newProxyInstance(
                dataSource.getClass().getClassLoader(),
                new Class<?>[]{DataSource.class},
                new DataSourceHandler(dataSource)
        );
    }

    @Override
    public Connection wrap(final Connection connection) {
        return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                new ConnectionHandler(connection)
        );
    }

    @Override
    public FlightRecorder recordJfrSnapshot() {
        try {
            final var configuration = Configuration.getConfiguration("profile");
            final var recording = new Recording(configuration);
            jfrOptions.eventNames().forEach(name -> recording.enable(name).withoutThreshold());
            recording.setMaxAge(jfrOptions.duration());
            recording.start();
            return new SimpleFlightRecorder(recording);
        } catch (final Throwable t) {
            logger.error("Failed to record JFR performance snapshot", t);
            return NoopFlightRecorder.INSTANCE;
        }
    }

    @Override
    public void record(final String name, final Runnable runnable) {
        try (final var recording = startRecording(name)) {
            try {
                runnable.run();
            } catch (final Throwable t) {
                recording.fail();
                throw t;
            }
        }
    }

    @Override
    public <T> T record(final String name, final Callable<T> callable) throws Exception {
        try (final var recording = startRecording(name)) {
            try {
                return callable.call();
            } catch (final Throwable t) {
                recording.fail();
                throw t;
            }
        }
    }

    @Override
    public TimeRecording startRecording(final String name) {
        return new SimpleTimeRecording(name);
    }

    private void stopAndRead(final Recording recording) {
        try (recording) {
            recording.stop();
            final var file = Files.createTempFile("faststats-performance-", ".jfr");
            try {
                recording.dump(file);
                readRecording(file);
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (final Throwable t) {
            logger.error("Failed to stop JFR performance recording", t);
        }
    }

    private void readRecording(final Path file) throws IOException {
        int count = 0;
        try (final var recordingFile = new RecordingFile(file)) {
            while (recordingFile.hasMoreEvents() && count < jfrOptions.maxEvents()) {
                count++;
                final var event = recordingFile.readEvent();
                if (!jfrOptions.eventNames().contains(event.getEventType().getName())) continue;
                if (!isAttributed(event)) continue;
                jfrStats.computeIfAbsent(event.getEventType().getName(), JfrStats::new).record(event);
            }
        }
    }

    private boolean isAttributed(final RecordedEvent event) {
        final var loader = jfrOptions.attributionLoader();
        if (loader == null || event.getStackTrace() == null) return true;
        for (final var frame : event.getStackTrace().getFrames()) {
            try {
                final var type = frame.getMethod().getType();
                final var clazz = Class.forName(type.getName(), false, loader);
                if (clazz.getClassLoader() == loader) return true;
            } catch (final Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean flush() {
        final var data = createData();
        if (data == null) return true;
        logger.info(data.toString());
        return true;
        // return submit(url, data, "performance");
    }

    @Override
    public void shutdown() {
        try {
            flush();
        } catch (final Throwable t) {
            logger.error("Failed to submit performance data on shutdown", t);
        }
    }

    private @Nullable JsonObject createData() {
        if (sqlStats.isEmpty() && jfrStats.isEmpty() && blockStats.isEmpty()) return null;

        final var performance = new JsonObject();
        final var sql = new JsonArray();
        sqlStats.values().forEach(stats -> sql.add(stats.toJson(sqlOptions.includeUpdateCounts())));
        if (!sql.isEmpty()) performance.add("sql", sql);

        final var jfr = new JsonArray();
        jfrStats.values().stream()
                .filter(stats -> !stats.hasZeroMillis())
                .forEach(stats -> jfr.add(stats.toJson()));
        if (!jfr.isEmpty()) performance.add("jfr", jfr);

        final var blocks = new JsonArray();
        blockStats.values().forEach(stats -> blocks.add(stats.toJson()));
        if (!blocks.isEmpty()) performance.add("blocks", blocks);

        sqlStats.clear();
        jfrStats.clear();
        blockStats.clear();

        final var data = new JsonObject();
        data.addProperty("project_name", context.getProjectName());
        data.addProperty("identifier", context.getConfig().serverId().toString());
        data.add("data", performance);
        return data;
    }

    private Statement wrapStatement(final Statement statement, final @Nullable String preparedSql) {
        final var interfaces = statement instanceof CallableStatement
                ? new Class<?>[]{CallableStatement.class}
                : statement instanceof PreparedStatement
                  ? new Class<?>[]{PreparedStatement.class}
                  : new Class<?>[]{Statement.class};
        return (Statement) Proxy.newProxyInstance(
                statement.getClass().getClassLoader(),
                interfaces,
                new StatementHandler(statement, preparedSql)
        );
    }

    private void recordSql(final String operation, final @Nullable String sql, final long nanos, final boolean failed, final long updateCount) {
        final var key = new QueryKey(operation, anonymizeSql(sql));
        final var target = sqlStats.containsKey(key) || sqlStats.size() < sqlOptions.maxQueryShapes()
                ? key
                : new QueryKey(operation, OVERFLOW_SHAPE);
        sqlStats.computeIfAbsent(target, SqlStats::new).record(nanos, failed, updateCount, sqlOptions.slowQueryThreshold());
    }

    private void recordBlock(final String name, final long nanos, final boolean failed) {
        blockStats.computeIfAbsent(name, BlockStats::new).record(nanos, failed);
    }

    private String anonymizeSql(final @Nullable String sql) {
        if (sql == null || sql.isBlank()) return "unknown";
        final var anonymized = ErrorHelper.applyAnonymization(sql, sqlAnonymizationEntries);
        return WHITESPACE.matcher(anonymized).replaceAll(" ").trim();
    }

    private static List<Map.Entry<Pattern, String>> sqlAnonymizationEntries(final List<Map.Entry<Pattern, String>> customEntries) {
        final var entries = new ArrayList<Map.Entry<Pattern, String>>();
        entries.add(Map.entry(STRING_LITERAL, "?"));
        entries.add(Map.entry(NUMBER_LITERAL, "?"));
        entries.addAll(customEntries);
        entries.add(Map.entry(WHITESPACE, " "));
        entries.add(Map.entry(IN_LIST, "in (?)"));
        return List.copyOf(entries);
    }

    private static String operation(final String methodName) {
        if (methodName.contains("Query")) return "query";
        if (methodName.contains("Update") || methodName.contains("LargeUpdate")) return "update";
        if (methodName.contains("Batch")) return "batch";
        return "execute";
    }

    private static @Nullable String sqlArgument(final Object @Nullable [] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof final String sql)) return null;
        return sql;
    }

    private static long updateCount(final Object result) {
        if (result instanceof final Number number) return number.longValue();
        if (result instanceof final int[] counts) {
            long total = 0;
            for (final var count : counts) if (count > 0) total += count;
            return total;
        }
        if (result instanceof final long[] counts) {
            long total = 0;
            for (final var count : counts) if (count > 0) total += count;
            return total;
        }
        return -1;
    }

    private static Object invoke(final Object target, final Method method, final Object @Nullable [] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    final class SimpleTimeRecording implements TimeRecording {
        private final String name;
        private final long start = System.nanoTime();
        private boolean stopped;

        private SimpleTimeRecording(final String name) {
            if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
            this.name = name;
        }

        @Override
        public void fail() {
            stop(true);
        }

        private void stop(final boolean failed) {
            if (stopped) return;
            stopped = true;
            recordBlock(name, System.nanoTime() - start, failed);
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
            stop(false);
        }
    }

    private final class DataSourceHandler implements InvocationHandler {
        private final DataSource dataSource;

        private DataSourceHandler(final DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object @Nullable [] args) throws Throwable {
            final var result = SimplePerformanceAnalyzer.invoke(dataSource, method, args);
            if ("getConnection".equals(method.getName()) && result instanceof final Connection connection) {
                return wrap(connection);
            }
            return result;
        }
    }

    private final class ConnectionHandler implements InvocationHandler {
        private final Connection connection;

        private ConnectionHandler(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object @Nullable [] args) throws Throwable {
            final var result = SimplePerformanceAnalyzer.invoke(connection, method, args);
            if (result instanceof final Statement statement) {
                final var name = method.getName();
                if ("createStatement".equals(name)) return wrapStatement(statement, null);
                if ("prepareStatement".equals(name) || "prepareCall".equals(name))
                    return wrapStatement(statement, sqlArgument(args));
            }
            return result;
        }
    }

    private final class StatementHandler implements InvocationHandler {
        private final Statement statement;
        private final @Nullable String preparedSql;

        private StatementHandler(final Statement statement, final @Nullable String preparedSql) {
            this.statement = statement;
            this.preparedSql = preparedSql;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object @Nullable [] args) throws Throwable {
            final var name = method.getName();
            if (!name.startsWith("execute")) return SimplePerformanceAnalyzer.invoke(statement, method, args);

            final var sql = preparedSql != null ? preparedSql : sqlArgument(args);
            final var start = System.nanoTime();
            var failed = false;
            try {
                final var result = SimplePerformanceAnalyzer.invoke(statement, method, args);
                recordSql(operation(name), sql, System.nanoTime() - start, false, updateCount(result));
                return result;
            } catch (final Throwable t) {
                failed = true;
                throw t;
            } finally {
                if (failed) recordSql(operation(name), sql, System.nanoTime() - start, true, -1);
            }
        }
    }

    final class SimpleFlightRecorder implements FlightRecorder {
        private final Recording recording;
        private volatile boolean closed;

        private SimpleFlightRecorder(final Recording recording) {
            this.recording = recording;
        }

        @Override
        public Recording recording() {
            return recording;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            stopAndRead(recording);
        }
    }

    static final class NoopFlightRecorder implements FlightRecorder {
        public static final NoopFlightRecorder INSTANCE = new NoopFlightRecorder();

        private NoopFlightRecorder() {
        }

        @Override
        public Recording recording() {
            return new Recording();
        }

        @Override
        public boolean isClosed() {
            return true;
        }

        @Override
        public void close() {
        }
    }

    private record QueryKey(String operation, String shape) {
    }

    private static final class SqlStats {
        private final QueryKey key;
        private long count;
        private long failures;
        private long slowCalls;
        private long totalNanos;
        private long minNanos = Long.MAX_VALUE;
        private long maxNanos;
        private long updateCount;

        private SqlStats(final QueryKey key) {
            this.key = key;
        }

        private void record(final long nanos, final boolean failed, final long updateCount, final Duration slowThreshold) {
            count++;
            if (failed) failures++;
            if (nanos >= slowThreshold.toNanos()) slowCalls++;
            totalNanos += nanos;
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
            if (updateCount > 0) this.updateCount += updateCount;
        }

        private JsonObject toJson(final boolean includeUpdateCounts) {
            final var json = new JsonObject();
            json.addProperty("operation", key.operation);
            json.addProperty("query_shape", key.shape);
            json.addProperty("count", count);
            json.addProperty("failures", failures);
            json.addProperty("slow_calls", slowCalls);
            json.addProperty("min_ms", TimeUnit.NANOSECONDS.toMicros(minNanos) / 1000.0D);
            json.addProperty("max_ms", TimeUnit.NANOSECONDS.toMicros(maxNanos) / 1000.0D);
            json.addProperty("avg_ms", TimeUnit.NANOSECONDS.toMicros(totalNanos / count) / 1000.0D);
            json.addProperty("total_ms", TimeUnit.NANOSECONDS.toMicros(totalNanos) / 1000.0D);
            if (includeUpdateCounts) json.addProperty("update_count", updateCount);
            return json;
        }
    }

    private static final class JfrStats {
        private final String event;
        private long count;
        private long totalNanos;
        private long minNanos = Long.MAX_VALUE;
        private long maxNanos;
        private @Nullable Instant firstSeen;
        private @Nullable Instant lastSeen;
        private final Map<ThreadKey, ThreadStats> threadStats = new HashMap<>();

        private JfrStats(final String event) {
            this.event = event;
        }

        private void record(final RecordedEvent event) {
            final var nanos = event.getDuration().toNanos();
            count++;
            totalNanos += nanos;
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
            firstSeen = firstSeen == null || event.getStartTime().isBefore(firstSeen) ? event.getStartTime() : firstSeen;
            lastSeen = lastSeen == null || event.getEndTime().isAfter(lastSeen) ? event.getEndTime() : lastSeen;
            final var thread = event.getThread();
            if (thread != null) {
                threadStats.computeIfAbsent(ThreadKey.from(thread), ThreadStats::new).record(nanos);
            }
        }

        private boolean hasZeroMillis() {
            return count == 0
                    || TimeUnit.NANOSECONDS.toMicros(totalNanos / count) == 0
                    && TimeUnit.NANOSECONDS.toMicros(maxNanos) == 0;
        }

        private JsonObject toJson() {
            final var json = new JsonObject();
            json.addProperty("event", event);
            json.addProperty("count", count);
            json.addProperty("min_ms", TimeUnit.NANOSECONDS.toMicros(minNanos) / 1000.0D);
            json.addProperty("avg_ms", count == 0 ? 0 : TimeUnit.NANOSECONDS.toMicros(totalNanos / count) / 1000.0D);
            json.addProperty("max_ms", TimeUnit.NANOSECONDS.toMicros(maxNanos) / 1000.0D);
            json.addProperty("total_ms", TimeUnit.NANOSECONDS.toMicros(totalNanos) / 1000.0D);
            if (firstSeen != null) json.addProperty("first_seen", firstSeen.toString());
            if (lastSeen != null) json.addProperty("last_seen", lastSeen.toString());
            if (!threadStats.isEmpty()) json.add("threads", threadsToJson());
            return json;
        }

        private JsonArray threadsToJson() {
            final var threads = new JsonArray();
            threadStats.values().stream()
                    .sorted(Comparator.comparingLong(ThreadStats::totalNanos).reversed())
                    .limit(10)
                    .forEach(stats -> threads.add(stats.toJson()));
            return threads;
        }
    }

    private record ThreadKey(String name, long javaThreadId, long osThreadId) {
        private static ThreadKey from(final RecordedThread thread) {
            return new ThreadKey(thread.getJavaName(), thread.getJavaThreadId(), thread.getOSThreadId());
        }
    }

    private static final class ThreadStats {
        private final ThreadKey key;
        private long count;
        private long totalNanos;
        private long minNanos = Long.MAX_VALUE;
        private long maxNanos;

        private ThreadStats(final ThreadKey key) {
            this.key = key;
        }

        private void record(final long nanos) {
            count++;
            totalNanos += nanos;
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
        }

        private long totalNanos() {
            return totalNanos;
        }

        private JsonObject toJson() {
            final var json = new JsonObject();
            json.addProperty("name", key.name);
            json.addProperty("java_thread_id", key.javaThreadId);
            json.addProperty("os_thread_id", key.osThreadId);
            json.addProperty("count", count);
            json.addProperty("min_ms", TimeUnit.NANOSECONDS.toMicros(minNanos) / 1000.0D);
            json.addProperty("avg_ms", TimeUnit.NANOSECONDS.toMicros(totalNanos / count) / 1000.0D);
            json.addProperty("max_ms", TimeUnit.NANOSECONDS.toMicros(maxNanos) / 1000.0D);
            json.addProperty("total_ms", TimeUnit.NANOSECONDS.toMicros(totalNanos) / 1000.0D);
            return json;
        }
    }

    private static final class BlockStats {
        private final String name;
        private long count;
        private long failures;
        private long totalNanos;
        private long minNanos = Long.MAX_VALUE;
        private long maxNanos;

        private BlockStats(final String name) {
            this.name = name;
        }

        private void record(final long nanos, final boolean failed) {
            count++;
            if (failed) failures++;
            totalNanos += nanos;
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
        }

        private JsonObject toJson() {
            final var json = new JsonObject();
            json.addProperty("name", name);
            json.addProperty("count", count);
            json.addProperty("failures", failures);
            json.addProperty("min_ms", TimeUnit.NANOSECONDS.toMicros(minNanos) / 1000.0D);
            json.addProperty("max_ms", TimeUnit.NANOSECONDS.toMicros(maxNanos) / 1000.0D);
            json.addProperty("avg_ms", TimeUnit.NANOSECONDS.toMicros(totalNanos / count) / 1000.0D);
            json.addProperty("total_ms", TimeUnit.NANOSECONDS.toMicros(totalNanos) / 1000.0D);
            return json;
        }
    }

    /**
     * Simple performance analyzer factory.
     *
     * @since 0.26.0
     */
    public static final class Factory implements PerformanceAnalyzer.Factory {
        private final SimpleContext context;
        private SqlPerformanceOptions sqlOptions = SqlPerformanceOptions.factory().create();
        private JfrPerformanceOptions jfrOptions = JfrPerformanceOptions.factory().create();

        public Factory(final SimpleContext context) {
            this.context = context;
        }

        @Override
        public Factory sql(final SqlPerformanceOptions options) {
            this.sqlOptions = options;
            return this;
        }

        @Override
        public Factory jfr(final JfrPerformanceOptions options) {
            this.jfrOptions = options;
            return this;
        }

        @Override
        public PerformanceAnalyzer create() {
            return new SimplePerformanceAnalyzer(this);
        }
    }
}
