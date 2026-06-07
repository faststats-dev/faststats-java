package dev.faststats.example;

import dev.faststats.FastStatsContext;
import dev.faststats.JfrPerformanceOptions;
import dev.faststats.PerformanceAnalyzer;
import dev.faststats.SimpleContext;
import dev.faststats.SqlPerformanceOptions;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Set;

import static dev.faststats.FlightEvents.EXECUTION_SAMPLE;
import static dev.faststats.FlightEvents.JAVA_MONITOR_ENTER;
import static dev.faststats.FlightEvents.SOCKET_READ;
import static dev.faststats.FlightEvents.SOCKET_WRITE;

public final class PerformanceAnalyzerExample {
    public static final FastStatsContext CONTEXT = getContextFactory()
            .performanceAnalyzer(factory -> factory
                    .sql(SqlPerformanceOptions.factory()
                            .slowQueryThreshold(Duration.ofMillis(150))
                            .maxQueryShapes(128)
                            .includeUpdateCounts(true)
                            .create())
                    .jfr(JfrPerformanceOptions.factory()
                            .duration(Duration.ofSeconds(5))
                            .eventNames(Set.of(EXECUTION_SAMPLE, JAVA_MONITOR_ENTER, SOCKET_READ, SOCKET_WRITE))
                            .maxEvents(5_000)
                            .attributionLoader(PerformanceAnalyzerExample.class.getClassLoader())
                            .create())
                    .create())
            .create();

    public static final PerformanceAnalyzer PERFORMANCE = CONTEXT.performanceAnalyzer().orElseThrow();

    public static void runInstrumentedQuery(final DataSource dataSource) throws SQLException {
        final var instrumented = PERFORMANCE.wrap(dataSource);

        try (final var connection = instrumented.getConnection();
             final var statement = connection.prepareStatement("UPDATE users SET last_seen = ? WHERE id = ?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setLong(2, 42L);
            statement.executeUpdate();
        }
    }

    public static void captureRuntimeSnapshot() {
        try (final var recorder = PERFORMANCE.recordJfrSnapshot()) {
            recorder.recording().setName("faststats-example");
            runCodeToProfile();
        } catch (final Throwable t) {
            PERFORMANCE.flush();
            throw t;
        }
    }

    public static String timeReturningMethod() throws Exception {
        return PERFORMANCE.record("load_profile", () -> loadProfile(42L));
    }

    public static void timeRunnable() {
        PERFORMANCE.record("refresh_cache", PerformanceAnalyzerExample::refreshCache);
    }

    public static void timeManualBlock() {
        try (final var recording = PERFORMANCE.startRecording("manual_block")) {
            try {
                runCodeToProfile();
            } catch (final Throwable t) {
                recording.fail();
                throw t;
            }
        }
    }

    private static void runCodeToProfile() {
    }

    private static String loadProfile(final long userId) {
        return "user-" + userId;
    }

    private static void refreshCache() {
    }

    private static SimpleContext.Factory<?, ?> getContextFactory() {
        return null;
    }
}
